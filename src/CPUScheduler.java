import java.util.*;

class Process {
    String name;
    int arrivalTime;
    int serviceTime;
    int priority; // optional, only used in AGING

    Process(String name, int arrivalTime, int serviceTime) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.serviceTime = serviceTime;
    }

    Process(String name, int arrivalTime, int serviceTime, int priority) {
        this(name, arrivalTime, serviceTime);
        this.priority = priority;
    }
}

class Algorithm {
    char id;
    int quantum;

    Algorithm(char id, int quantum) {
        this.id = id;
        this.quantum = quantum;
    }
}

public class CPUScheduler {
    static final String TRACE = "trace";
    static final String STATS = "stats";
    static List<Process> processes = new ArrayList<>();
    static List<Algorithm> algorithms = new ArrayList<>();
    static int lastInstant;
    static int processCount;
    static char[][] timeline;
    static int[] finishTime;
    static int[] turnAroundTime;
    static double[] normTurn;
    static String operation;

    public static void main(String[] args) {
        parseInput();

        for (Algorithm algo : algorithms) {
            clearTimeline();
            executeAlgorithm(algo.id, algo.quantum);
            if (operation.equals(TRACE)) printTimeline();
            else printStats();
            System.out.println();
        }
    }

    static void parseInput() {
        operation = TRACE;
        algorithms.add(new Algorithm('1', 0)); // FCFS
        algorithms.add(new Algorithm('2', 2)); // RR with quantum 2
        algorithms.add(new Algorithm('3', 0)); // SPN
        algorithms.add(new Algorithm('5', 0)); // HRRN
        lastInstant = 20;
        processes.add(new Process("P1", 0, 4));
        processes.add(new Process("P2", 1, 3));
        processes.add(new Process("P3", 2, 2));
        processCount = processes.size();
        timeline = new char[lastInstant][processCount];
        finishTime = new int[processCount];
        turnAroundTime = new int[processCount];
        normTurn = new double[processCount];
    }

    static void clearTimeline() {
        for (int i = 0; i < lastInstant; i++)
            Arrays.fill(timeline[i], ' ');
        Arrays.fill(finishTime, 0);
        Arrays.fill(turnAroundTime, 0);
        Arrays.fill(normTurn, 0);
    }

    static void executeAlgorithm(char id, int quantum) {
        switch (id) {
            case '1': runFCFS(); break;
            case '2': runRR(quantum); break;
            case '3': runSPN(); break;
            case '5': runHRRN(); break;
        }
    }

    static void runFCFS() {
        int time = processes.get(0).arrivalTime;
        for (int i = 0; i < processCount; i++) {
            Process p = processes.get(i);
            int start = Math.max(time, p.arrivalTime);
            int end = start + p.serviceTime;
            finishTime[i] = end;
            turnAroundTime[i] = end - p.arrivalTime;
            normTurn[i] = (double) turnAroundTime[i] / p.serviceTime;
            for (int j = start; j < end && j < lastInstant; j++)
                timeline[j][i] = '*';
            for (int j = p.arrivalTime; j < start && j < lastInstant; j++)
                timeline[j][i] = '.';
            time = end;
        }
    }

    static void runRR(int quantum) {
        Queue<int[]> queue = new LinkedList<>(); // {processIndex, remainingTime}
        int j = 0, time = 0;
        while (j < processCount && processes.get(j).arrivalTime <= time) {
            queue.add(new int[]{j, processes.get(j).serviceTime});
            j++;
        }

        while (time < lastInstant) {
            if (!queue.isEmpty()) {
                int[] current = queue.poll();
                int index = current[0];
                int remaining = current[1];
                Process p = processes.get(index);
                int served = Math.min(quantum, remaining);
                for (int t = 0; t < served && time < lastInstant; t++) {
                    timeline[time++][index] = '*';
                }
                remaining -= served;

                while (j < processCount && processes.get(j).arrivalTime <= time) {
                    queue.add(new int[]{j, processes.get(j).serviceTime});
                    j++;
                }

                if (remaining > 0) {
                    queue.add(new int[]{index, remaining});
                } else {
                    finishTime[index] = time;
                    turnAroundTime[index] = time - p.arrivalTime;
                    normTurn[index] = (double) turnAroundTime[index] / p.serviceTime;
                }
            } else {
                time++;
                while (j < processCount && processes.get(j).arrivalTime <= time) {
                    queue.add(new int[]{j, processes.get(j).serviceTime});
                    j++;
                }
            }
        }
    }

    static void runSPN() {
        boolean[] done = new boolean[processCount];
        int time = 0;
        while (time < lastInstant) {
            int selected = -1;
            int minService = Integer.MAX_VALUE;
            for (int i = 0; i < processCount; i++) {
                Process p = processes.get(i);
                if (!done[i] && p.arrivalTime <= time && p.serviceTime < minService) {
                    selected = i;
                    minService = p.serviceTime;
                }
            }
            if (selected == -1) {
                time++;
                continue;
            }
            Process p = processes.get(selected);
            for (int t = 0; t < p.serviceTime && time < lastInstant; t++) {
                timeline[time++][selected] = '*';
            }
            finishTime[selected] = time;
            turnAroundTime[selected] = time - p.arrivalTime;
            normTurn[selected] = (double) turnAroundTime[selected] / p.serviceTime;
            done[selected] = true;
        }
    }

    static void runHRRN() {
        boolean[] done = new boolean[processCount];
        int time = 0;
        while (time < lastInstant) {
            double maxRR = -1;
            int selected = -1;
            for (int i = 0; i < processCount; i++) {
                Process p = processes.get(i);
                if (!done[i] && p.arrivalTime <= time) {
                    int wait = time - p.arrivalTime;
                    double rr = (double)(wait + p.serviceTime) / p.serviceTime;
                    if (rr > maxRR) {
                        maxRR = rr;
                        selected = i;
                    }
                }
            }
            if (selected == -1) {
                time++;
                continue;
            }
            Process p = processes.get(selected);
            for (int t = 0; t < p.serviceTime && time < lastInstant; t++) {
                timeline[time++][selected] = '*';
            }
            finishTime[selected] = time;
            turnAroundTime[selected] = time - p.arrivalTime;
            normTurn[selected] = (double) turnAroundTime[selected] / p.serviceTime;
            done[selected] = true;
        }
    }

    static void printTimeline() {
        for (int i = 0; i < lastInstant; i++)
            System.out.print(i % 10 + " ");
        System.out.println("\n" + "-".repeat(lastInstant * 2));
        for (int i = 0; i < processCount; i++) {
            System.out.print(processes.get(i).name + " |");
            for (int j = 0; j < lastInstant; j++)
                System.out.print(timeline[j][i] + "|");
            System.out.println();
        }
        System.out.println("-".repeat(lastInstant * 2));
    }

    static void printStats() {
        System.out.print("Process:     ");
        for (Process p : processes) System.out.printf("%5s ", p.name);
        System.out.print("\nFinishTime:  ");
        for (int f : finishTime) System.out.printf("%5d ", f);
        System.out.print("\nTurnAround:  ");
        for (int t : turnAroundTime) System.out.printf("%5d ", t);
        System.out.print("\nNormTurn:    ");
        for (double nt : normTurn) System.out.printf("%5.2f ", nt);
        System.out.println();
    }
}
