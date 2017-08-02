package org.atlasapi.reporting.telescope;

import java.math.BigInteger;

import com.metabroadcast.columbus.telescope.api.Process;
import com.metabroadcast.columbus.telescope.api.Task;
import com.metabroadcast.columbus.telescope.client.IngestTelescopeClientImpl;
import com.metabroadcast.columbus.telescope.client.TelescopeClientImpl;
import com.metabroadcast.common.ids.SubstitutionTableNumberCodec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TelescopeProxy {

    private static final Logger log = LoggerFactory.getLogger(TelescopeProxy.class);

    //check for null before use, as it might fail to initialize
    protected IngestTelescopeClientImpl telescopeClient;

    protected String taskId;
    protected Process process;
    protected ObjectMapper objectMapper;
    protected boolean startedReporting = false; //safeguard flags
    protected boolean stoppedReporting = false;
    protected SubstitutionTableNumberCodec idCodec; //used to create atlasIDs

    /**
     * The client always reports to {@link TelescopeConfiguration#TELESCOPE_HOST}
     */
    protected TelescopeProxy(Process process) {
        this.process = process;

        //the telescope client might fail to initialize, in which case it will remain null,
        // and thus and we'll have to check for that in further operations.
        TelescopeClientImpl client = TelescopeClientImpl.create(TelescopeConfiguration.TELESCOPE_HOST);
        if (client == null) { //precaution, not sure if it can actually happen.
            log.error(
                    "Could not get a TelescopeClientImpl object with the given TELESCOPE_HOST={}",
                    TelescopeConfiguration.TELESCOPE_HOST
            );
            log.error(
                    "This telescope proxy will not report to telescope, and will not print any further messages.");
        } else {
            this.telescopeClient = IngestTelescopeClientImpl.create(client);
            this.objectMapper = new ObjectMapper();
        }
    }

    /**
     * Make the telescope aware that a new process has started reporting.
     *
     * @return Returns true on success, and false on failure.
     */
    public boolean startReporting() {
        //do we have a telescope client?
        if (!isInitialized()) {
            return false;
        }
        //make sure we have not already done that
        if (startedReporting) {
            log.warn(
                    "Someone tried to start a telescope report through a proxy that had already started reporting., taskId={}",
                    taskId
            );
            return false;
        }

        Task task = telescopeClient.startIngest(process);
        if (task.getId().isPresent()) {
            taskId = task.getId().get();
            startedReporting = true;
            log.info("Started reporting to Telescope, taskId={}", taskId);
            return true;
        } else {
            //this log might be meaningless, because I might not be understanding under
            // which circumstances this id might be null.
            log.error("Reporting a Process to telescope did not respond with a taskId");
            return false;
        }
    }

    /**
     * Let telescope know we are finished reporting through this proxy. Once finished this object is
     * useless.
     */
    public void endReporting() {
        if (!isInitialized()) {
            return;
        }
        if (startedReporting) {
            telescopeClient.endIngest(taskId);
            stoppedReporting = true;
            log.info("Finished reporting to Telescope, taskId={}", taskId);
        } else {
            log.warn("Someone tried to stop a telescope report that has never started");
        }
    }

    //To allow for better logging down the line
    public String getTaskId() {
        return taskId;
    }

    public String getIngesterName() {
        return this.process.getKey();
    }

    protected boolean isInitialized() {
        return (telescopeClient != null);
    }

    // This allows us to work with db id's instead of atlas ids,
    // without forcing each caller to create his own encoder.
    // This is here and not in the utility class so we dont recreate the codec object all the time.
    protected String encode(Long id) {
        try { //we do this because we want to print a proper stack, so we can find who caused this error.
            if (id == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            log.error(
                    "Someone attempted to convert a null into an atlas id. We returned null and went on with our lives.",
                    e
            );
            return null;
        }

        //lazy initialize
        if (this.idCodec == null) {
            this.idCodec = SubstitutionTableNumberCodec.lowerCaseOnly();
        }
        return idCodec.encode(BigInteger.valueOf(id));
    }

}
