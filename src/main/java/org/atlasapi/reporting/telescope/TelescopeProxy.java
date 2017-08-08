package org.atlasapi.reporting.telescope;

import java.math.BigInteger;

import com.metabroadcast.columbus.telescope.api.Event;
import com.metabroadcast.columbus.telescope.api.Process;
import com.metabroadcast.columbus.telescope.api.Task;
import com.metabroadcast.columbus.telescope.client.IngestTelescopeClientImpl;
import com.metabroadcast.columbus.telescope.client.TelescopeClientImpl;
import com.metabroadcast.common.ids.SubstitutionTableNumberCodec;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is just a more convenient interface for reporting to telescope. It groups together the
 * initialization of the telescope client and offers some safeguards for order events can happen.
 * The object is self contained, so you can pass it around and just call the reporting methods when
 * appropriate. More specific implementations should extend this class to offer methods for the
 * actual reporting of events, as this abstract class only deals with starting and ending a new
 * reporting process.
 * <p>
 * The order of things should be create a proxy, startReporting, reportEvent (through any methods offered by
 * the particular implementation), and finally endReporting.
 */
public abstract class TelescopeProxy {

    private static final Logger log = LoggerFactory.getLogger(TelescopeProxy.class);

    //check with .isInitialized() before use, as it might have failed to initialize
    private IngestTelescopeClientImpl telescopeClient;

    private String taskId;
    private Process process;
    private boolean startedReporting = false; //safeguard flags
    private boolean finishedReporting = false;
    protected SubstitutionTableNumberCodec idCodec; //used to create an atlasId from DBid

    protected TelescopeProxy(Process process) {
        this.process = process;

        //the telescope client might fail to initialize, in which case it will remain null,
        // and thus and we'll have to check for that in further operations.
        TelescopeClientImpl client = TelescopeClientImpl.create(TelescopeConfiguration.TELESCOPE_HOST);
        if (client == null) { //precaution, not sure if it can actually happen.
            log.error(
                    "Could not get a TelescopeClientImpl object with the given TELESCOPE_HOST={}" +
                    "This telescope proxy will not report to telescope, and will not print any further messages.",
                    TelescopeConfiguration.TELESCOPE_HOST
            );
        } else {
            this.telescopeClient = IngestTelescopeClientImpl.create(client);
        }
    }

    /**
     * Make the telescope aware that a new process has started reporting. The only reason this block
     * is here and not in the constructor, is to make consumers aware that they should endReporting.
     */
    public void startReporting() {
        if (!isInitialized()) {
            return; //an error message was printed when initialization failed.
        }
        if (isStarted()) {
            log.warn( "This telescope proxy has already been started. It was not started again. taskId={}", taskId );
            return;
        }

        Task task = telescopeClient.startIngest(process);
        if (task.getId().isPresent()) {
            taskId = task.getId().get();
            startedReporting = true;
            log.debug("Started reporting to Telescope, taskId={}", taskId);
        } else {
            //this log might be meaningless, because I might not be understanding under
            // which circumstances this id might be null, if any.
            log.error("Reporting a Process to telescope did not respond with a taskId");
        }
    }

    public void reportEvent(Event event){
        if(isInitialized()){
            telescopeClient.createEvents(ImmutableList.of(event));
        }
        else{
//            log.debug("It was attempted to report an event to telescope through a client that is not initialized. Reporter name:{}", getReporterName());
        }
    }


    /**
     * Let telescope know we are finished reporting through this proxy. Surprisingly, telescope
     * seems to accept events even after it is notified that reporting has finished.
     */
    public void endReporting() {
        if (!isInitialized()) {
            return;
        }
        if (isStarted()) {
            telescopeClient.endIngest(taskId);
            finishedReporting = true;
            log.debug("Finished reporting to Telescope, taskId={}", taskId);
        } else {
            log.warn("Someone tried to stop a telescope report that has never started");
        }
    }

    //To allow for better logging down the line
    public String getTaskId() {
        return taskId;
    }

    public String getReporterName() {
        return this.process.getKey();
    }

    /**
     * If it has started reporting, then it is initialized. Since this is the first step, all
     * methods should check against this before doing anything.
     *
     * @return
     */
    protected boolean isStarted() {
        return startedReporting;
    }

    protected boolean isFinished() {
        return finishedReporting;
    }

    protected boolean isInitialized() {
        return (telescopeClient != null);
    }

    // This allows us to work with db id's instead of atlas ids,
    // without forcing each caller to create his own encoder.
    // This is here and not in the utility class so we dont recreate the codec object all the time.
    protected String encode(Long id) {
        if (id == null) {
            return null;
        }

        //lazy initialize
        if (this.idCodec == null) {
            this.idCodec = SubstitutionTableNumberCodec.lowerCaseOnly();
        }

        return idCodec.encode(BigInteger.valueOf(id));
    }

}
