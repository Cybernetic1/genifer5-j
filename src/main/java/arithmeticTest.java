/**
 * Created by yky on 10/9/15.
 */

// **************** 2-Digit Primary-school Subtraction Arithmetic test *****************

// The goal is to perform subtraction like a human child would.
// Input: 2-digit numbers A and B, for example "12", "07"
// Output: A - B, eg:  "12" - "07" = "05"

// State vector = [ A1, A0, B1, B0, C1, C0, carry-flag, current-digit, result-ready-flag,
//		underflow-error-flag ]

// Algorithm:

// If current-digit = 0:
//		if A0 >= B0 then C0 = A0 - B0
//		else C0 = 10 + (A0 - B0) , carry-flag = 1
//		current-digit = 1

// If current-digit = 1:
//		if A1 >= B1 then
//			C1 = A1 - B1
//		else Underflow Error
//		if carry-flag = 0:
//			result-ready = 1
//		else	// carry-flag = 1
//			if C1 >= 1
//				--C1
//			else Underflow error
//			result-ready = 1

public class arithmeticTest
	{

	public static void main(final String args[])
		{
		System.out.println("Arithmetic test\n");
		arithmetic_testA();
		}

	// This defines the transition operator acting on vector space K1 (of dimension 10)
	public static void transition(Double K1[], Double K2[])
		{
		Double A1 = Math.floor(K1[0] * 10.0) / 10.0;
		Double A0 = Math.floor(K1[1] * 10.0) / 10.0;
		Double B1 = Math.floor(K1[2] * 10.0) / 10.0;
		Double B0 = Math.floor(K1[3] * 10.0) / 10.0;
		Double carryFlag = K1[4];
		Double currentDigit = K1[5];
		Double C1 = K1[6];
		Double C0 = K1[7];
		Double resultReady = K1[8];
		Double underflowError = K1[9];

		// System.out.println("A1,A0 = " + A1.toString() + A0.toString());
		// System.out.println("B1,B0 = " + B1.toString() + B0.toString());

		if (currentDigit < 0.5)
			{
			if (A0 >= B0)                // C seems to support >= for comparison of doubles
				{
				C0 = A0 - B0;
				carryFlag = 0.0;
				}
			else
				{
				C0 = 1.0 + (A0 - B0);
				carryFlag = 1.0;
				}
			currentDigit = 1.0;
			resultReady = 0.0;
			underflowError = 0.0;
			C1 = 0.0;                    // optional
			}
		else        // current digit = 1
			{
			resultReady = 1.0;

			if (A1 >= B1)
				{
				C1 = A1 - B1;
				underflowError = 0.0;
				}
			else
				{
				underflowError = 1.0;
				C1 = 0.0;                // optional
				}

			if (carryFlag > 0.5)
				{
				if (C1 > 0.09999)
					C1 -= 0.1;
				else
					underflowError = 1.0;
				}

			C0 = C0;                    // necessary
			carryFlag = 0.0;            // optional
			currentDigit = 1.0;            // optional
			}

		K2[0] = A1;
		K2[1] = A0;
		K2[2] = B1;
		K2[3] = B0;
		K2[4] = carryFlag;
		K2[5] = currentDigit;
		K2[6] = C1;
		K2[7] = C0;
		K2[8] = resultReady;
		K2[9] = underflowError;
		}

	private static char digit(double d)
		{
		int i = (int) (d * 10.0 + 0.0001);
		// System.out.println(Integer.toString(i));
		char c = '0';
		c += i;
		// System.out.println(Integer.toString(c));
		return c;
		}

	// Test the transition operator (1 time)
	// This tests both the arithmetic of the digits as well as the settings of flags.
	public static void arithmetic_testA_1()
		{
		Double[] K1 = new Double[10];
		Double[] K2 = new Double[10];
		Double a1, a0, b1, b0;

		// generate A,B randomly
		java.util.Random r = new java.util.Random();
		a1 = Math.floor(r.nextDouble() * 10.0) / 10.0;
		K1[0] = a1;
		a0 = Math.floor(r.nextDouble() * 10.0) / 10.0;
		K1[1] = a0;
		b1 = Math.floor(r.nextDouble() * 10.0) / 10.0;
		K1[2] = b1;
		b0 = Math.floor(r.nextDouble() * 10.0) / 10.0;
		K1[3] = b0;

		// System.out.println("a1,a0 = " + a1.toString() + "," + a0.toString());
		// System.out.println("b1,b0 = " + b1.toString() + "," + b0.toString());

		// System.out.println("chars: " + digit(a1) + digit(a0) + " - " + digit(b1) + digit(b0));

		// correct answer
		Integer a = (int) (a1 * 10.0) * 10 + (int) (a0 * 10.0);
		Integer b = (int) (b1 * 10.0) * 10 + (int) (b0 * 10.0);
		System.out.print(a.toString() + " - ");
		System.out.print(b.toString() + " = ");

		Integer c = a - b;
		Double c1 = (double) (c / 10) / 10.0;
		Double c0 = (double) (c % 10) / 10.0;
		System.out.println(c.toString());

		K1[4] = 0.0;		// carry flag
		K1[5] = 0.0;		// current digit
		K1[8] = 0.0;		// result-ready
		K1[9] = 0.0;		// underflow

		Integer loop = 0;
		while (loop < 2)			// The transition operator should operate 2 times
			{
			// call the transition
			transition(K1, K2);
			++loop;

			//System.out.println("result-ready = " + K2[8].toString());

			// get result
			if (K2[8] > 0.5)            // result ready?
				{
				boolean correct = true;

				if (c < 0)                // result is negative
					{
					if (K2[9] < 0.5)    // underflow should be set but is not
						correct = false;
					}
				else
					{
					if (K2[9] > 0.5)    // underflow should be clear but is set
						correct = false;

					double err1 = Math.abs(K2[6] - c1);
					double err2 = Math.abs(K2[7] - c0);
					if (err1 > 0.001)
						correct = false;
					if (err2 > 0.001)
						correct = false;
					}

				System.out.println(" answer = " + digit(c1) + digit(c0));
				System.out.println(" genifer = " + digit(K2[6]) + digit(K2[7]));

				if (correct)
					System.out.println
							("\33[32m*********** Correct \33[39m\n");
				else
					{
					System.out.println("\33[31mWrong!!!!!! ");
					System.out.println("underflow = " + K2[9].toString() + "\33[39m\n");
					// beep();
					}
				}
			else
				{
				// Copy output K vector to input, for the next iteration
				for (int k = 0; k < 10; ++k)
					K1[k] = K2[k];
				}
			}
		}

	// Repeat the test N times
	public static void arithmetic_testA()
		{
		for (Integer n = 0; n < 50; ++n)
			{
			System.out.print("(" + n.toString() + ") ");
			arithmetic_testA_1();
			}
		}

	}
