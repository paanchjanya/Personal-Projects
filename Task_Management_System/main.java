import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Task {
    private String title;
    private String description;
    private boolean completed;

    public Task(String title, String description) {
        this.title = title;
        this.description = description;
        this.completed = false;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markAsCompleted() {
        this.completed = true;
    }

    @Override
    public String toString() {
        return "Title: " + title + "\nDescription: " + description + "\nStatus: " + (completed ? "Completed" : "Pending");
    }
}

public class TaskManager {
    private List<Task> tasks;
    private Scanner scanner;

    public TaskManager() {
        tasks = new ArrayList<>();
        scanner = new Scanner(System.in);
    }

    public void addTask() {
        System.out.print("Enter task title: ");
        String title = scanner.nextLine();
        System.out.print("Enter task description: ");
        String description = scanner.nextLine();

        Task newTask = new Task(title, description);
        tasks.add(newTask);
        System.out.println("Task added successfully!");
    }

    public void viewTasks() {
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }

        System.out.println("\n--- Tasks ---");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println("\nTask " + (i + 1) + ":");
            System.out.println(tasks.get(i));
        }
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Task {
    private String title;
    private String description;
    private boolean completed;

    public Task(String title, String description) {
        this.title = title;
        this.description = description;
        this.completed = false;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markAsCompleted() {
        this.completed = true;
    }

    @Override
    public String toString() {
        return "Title: " + title + "\nDescription: " + description + "\nStatus: " + (completed ? "Completed" : "Pending");
    }
}

public class TaskManager {
    private List<Task> tasks;
    private Scanner scanner;

    public TaskManager() {
        tasks = new ArrayList<>();
        scanner = new Scanner(System.in);
    }

    public void addTask() {
        System.out.print("Enter task title: ");
        String title = scanner.nextLine();
        System.out.print("Enter task description: ");
        String description = scanner.nextLine();

        Task newTask = new Task(title, description);
        tasks.add(newTask);
        System.out.println("Task added successfully!");
    }

    public void viewTasks() {
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }

        System.out.println("\n--- Tasks ---");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println("\nTask " + (i + 1) + ":");
            System.out.println(tasks.get(i));
        }
        System.out.println("-------------\n");
    }

    public void markTaskAsCompleted() {
        viewTasks(); // Show tasks with indices

        if (tasks.isEmpty()) {
            return;
        }

        System.out.print("Enter the task number to mark as completed: ");
        try {
            int taskNumber = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if (taskNumber > 0 && taskNumber <= tasks.size()) {
                Task taskToComplete = tasks.get(taskNumber - 1);
                taskToComplete.markAsCompleted();
                System.out.println("Task marked as completed!");
            } else {
                System.out.println("Invalid task number.");
            }
        } catch (java.util.InputMismatchException e) {
            System.out.println("Invalid input. Please enter a number.");
            scanner.nextLine(); // Clear the invalid input from the scanner
        }
    }


    public static void main(String[] args) {
        TaskManager taskManager = new TaskManager();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nTask Manager Menu:");
            System.out.println("1. Add Task");
            System.out.println("2. View Tasks");
            System.out.println("3. Mark Task as Completed");
            System.out.println("4. Exit");

            System.out.print("Enter your choice: ");
            try{
                 int choice = scanner.nextInt();
                 scanner.nextLine();

                 switch (choice) {
                    case 1:
                        taskManager.addTask();
                        break;
                    case 2:
                        taskManager.viewTasks();
                        break;
                    case 3:
                        taskManager.markTaskAsCompleted();
                        break;
                    case 4:
                        System.out.println("Exiting Task Manager.");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }

            }catch(java.util.InputMismatchException e){
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
            }

        }
    }
}
