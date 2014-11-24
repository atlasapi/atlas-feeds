package org.atlasapi.feeds.youview.tasks.simple;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.atlasapi.media.vocabulary.PLAY_SIMPLE_XML;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

@XmlRootElement(namespace=PLAY_SIMPLE_XML.NS, name="tasks")
@XmlType(name="tasks", namespace=PLAY_SIMPLE_XML.NS)
public class TaskQueryResult {
    
    private List<Task> tasks = Lists.newArrayList();

    public void add(Task task) {
        tasks.add(task);
    }

    @XmlElements({ 
        @XmlElement(name = "task", type = Task.class, namespace=PLAY_SIMPLE_XML.NS)
    })
    public List<Task> getTasks() {
        return tasks;
    }
    
    public void setTasks(Iterable<Task> transactions) {
        this.tasks = Lists.newArrayList(transactions);
    }
    
    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    @Override
    public int hashCode() {
        return tasks.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this instanceof TaskQueryResult) {
            TaskQueryResult other = (TaskQueryResult) obj;
            return tasks.equals(other.tasks);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(tasks)
                .toString();
    }
}
