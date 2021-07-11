package portalStones.utilities;

import java.util.Random;

public class RNG {
	private static Random random = new Random();

	public static float Random(float a, float b) {
		double f = (random.nextInt() - -2.147483648E9D) / 4.294967295E9D;
		return (float) (a * (1.0D - f) + b * f);
	}

	public static int Random(int a, int b) {
		double f = (random.nextInt() - -2.147483648E9D) / 4.294967295E9D;
		int roll = (int) (a * (1.0D - f) + (b + 1.0D) * f);
		return (roll > b) ? b : roll;
	}

	public static double Random(double a, double b) {
		double f = (random.nextInt() - -2.147483648E9D) / 4.294967295E9D;
		return a * (1.0D - f) + b * f;
	}

	public static boolean Random() {
		return !(Random(0, 1) == 0);
	}
}
