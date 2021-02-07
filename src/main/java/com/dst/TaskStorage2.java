package com.dst;

import com.dst.msg.WarehouseMessage;
import com.dst.users.DriverStatus;
import com.dst.users.UserDriver;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TaskStorage2 {

    static Comparator<WarehouseMessage.Task2OrBuilder> comparator = Comparator
            .comparing(WarehouseMessage.Task2OrBuilder::getPriority)
            .reversed()
            .thenComparing(WarehouseMessage.Task2OrBuilder::getId);

    public static Set<WarehouseMessage.Task2> allTasks = new ConcurrentSkipListSet<>(comparator);

    public static CopyOnWriteArrayList<UserDriver> drivers = new CopyOnWriteArrayList<>();

    public static void driverFree(UserDriver userDriver){

        //check high prior -> check mass
        //check med prior -> check mass
        //check low
    }

    private Set<WarehouseMessage.Task2> getPrior(Set<WarehouseMessage.Task2> tasksSet, WarehouseMessage.NewTask.Priority priority){
        return tasksSet.stream()
                .filter(t -> t.getPriority() == priority)
                .collect(Collectors.toSet());
    }

    private Set<WarehouseMessage.Task2> getWeight (Set<WarehouseMessage.Task2> tasksSet, WarehouseMessage.NewTask.Weight weight) {
        return tasksSet.stream()
                .filter(t -> t.getWeight() == weight)
                .collect(Collectors.toSet());
    }

    private WarehouseMessage.Task2 getTaskForDriver(UserDriver driver){
        for (int i = WarehouseMessage.NewTask.Priority.HIGH_VALUE; i >= 0; i--){
            for (int j = driver.getWeightClass().getNumber(); j >= 0; j--){
                TreeSet<WarehouseMessage.Task2> set = new TreeSet<>(comparator);
                set.addAll(getWeight(getPrior(allTasks, WarehouseMessage.NewTask.Priority.forNumber(i)), WarehouseMessage.NewTask.Weight.forNumber(j)));
                if (!set.isEmpty()) return set.first();
            }
        }
        return null;
    }

    private WarehouseMessage.Task2 checkTaskBeforeAdd(WarehouseMessage.Task2 task){
        if (isFreeDriverForTaskExist(task)){
            for (int i = task.getWeightValue(); i <= WarehouseMessage.NewTask.Weight.KG_5000_VALUE; i++){
                Set<UserDriver> set = getWeightDrivers(WarehouseMessage.NewTask.Weight.forNumber(i));
                if(!set.isEmpty()) return task.toBuilder().setStatus(WarehouseMessage.Task2.Status.STARTED).setReporter(set.iterator().next().getUserName()).build();
            }
        }
        return task;

    }

    private boolean isFreeDriverExist(){
        return drivers.stream().anyMatch(d -> d.getStatus() == DriverStatus.FREE);
    }

    private Set<UserDriver> getWeightDrivers(WarehouseMessage.NewTask.Weight weight){
        return drivers.stream().filter(d -> d.getWeightClass() == weight).collect(Collectors.toSet());
    }

    private boolean isFreeDriverForTaskExist(WarehouseMessage.Task2 task){
        return drivers.stream()
                .filter(d -> d.getWeightClass().getNumber() <= task.getWeight().getNumber())
                .anyMatch(d -> d.getStatus() == DriverStatus.FREE);
    }

    private void updateTask(WarehouseMessage.Task2 task){
        allTasks.remove(getTaskById(task.getId()));
        allTasks.add(task);
    }

    public WarehouseMessage.Task2 getTaskById(int id){
        return allTasks.stream().filter(t -> t.getId() == id).findAny().orElse(null);
    }

    //dispatcher adds task
    public void addTask(WarehouseMessage.Task2 task){
        allTasks.add(checkTaskBeforeAdd(task));
        //update all listeners
    }

    public void finishTask(WarehouseMessage.Task2 task){
        // updateTask - FINISHED
        // update all listeners
        // remove task from set
    }

    public void cancelTask(WarehouseMessage.Task2 task){
        // updateTask - WAIT
        // update all listeners
    }
}
