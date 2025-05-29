package beastlabs.math.distributions;

/**
 * A composition is an ordered sequence of integers whose sum is n.
 * k is the number of integers into which n is divided.
 */
public class CompositionCounter {

    // Computes n choose k (nCk)
    public static long binomial(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        long result = 1;
        for (int i = 1; i <= k; i++) {
            result *= (n - (k - i));
            result /= i;
        }
        return result;
    }

    // Number of compositions into k non-negative integers that sum to n
    public static long countCompositionsNonNegative(int n, int k) {
        return binomial(n + k - 1, k - 1);
    }

    // Number of compositions into k positive integers that sum to n
    public static long countCompositionsPositive(int n, int k) {
        if (k > n) return 0; // Not enough to give at least 1 to each
        return binomial(n - 1, k - 1);
    }

    public static void main(String[] args) {
        int n = 5, k = 3;
        System.out.println("Non-negative compositions: " + countCompositionsNonNegative(n, k)); // Output: 21
        System.out.println("Positive compositions: " + countCompositionsPositive(n, k));       // Output: 6
        n = 3; k = 2;
        // Output: 4 {(1,2),(2,1),(0,3),(3,0)}
        System.out.println("Non-negative compositions: " + countCompositionsNonNegative(n, k));
        // Output: 2 {(1,2),(2,1)}
        System.out.println("Positive compositions: " + countCompositionsPositive(n, k));
    }
}