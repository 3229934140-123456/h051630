package example;

public class TargetClass {
    public static long totalTime = 0;

    public int add(int a, int b) {
        int result = a + b;
        return result;
    }

    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        TargetClass tc = new TargetClass();
        System.out.println("Result: " + tc.add(10, 20));
        System.out.println(tc.greet("World"));
    }
}
