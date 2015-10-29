/**
 * Created by yky on 10/9/15.
 */

import org.encog.Encog;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.mathutil.randomize.ConsistentRandomizer;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.persist.EncogDirectoryPersistence;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import java.io.*;
import java.util.Arrays;

// Similar to Test1, with alternative representation of numbers

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

public class arithmeticTest2
	{
	public static void beep()
		{
		// open the sound file as a Java input stream
		String gongFile = "beep.au";
		InputStream in = null;
		try
			{
			in = new FileInputStream(gongFile);
			} catch (FileNotFoundException e)
			{
			e.printStackTrace();
			}
		// create an audio stream from the input stream
		AudioStream audioStream = null;
		try
			{
			audioStream = new AudioStream(in);
			} catch (IOException e)
			{
			e.printStackTrace();
			}
		// play the audio clip with the audio player class
		AudioPlayer.player.start(audioStream);
		}

	public static void main(final String args[]) throws IOException, ClassNotFoundException
		{
		beep();
		int c = '!';
		while (c != 'q')
			{
			System.out.println("\nArithmetic test 2\n");
			System.out.println("[0] test arithmetic operator\n");
			System.out.println("[1] train network\n");
			System.out.println("[2] test network\n");
			System.out.println("[q] quit\n");
			do
				c = System.in.read();
			while (!Character.isLetterOrDigit(c));

			switch (c)
				{
				case '0':
					arithmetic_testA();
					break;
				case '1':
					arithmetic_testB();
					break;
				case '2':
					arithmetic_testC();
					break;
				default:
					break;
				}
			}
		}

	// This defines the transition operator acting on vector space K1 (of dimension 10)
	public static void transition(double K1[], double K2[])
		{
		// In this version, the 2 numbers A and B belong to [0,100] and are represented
		// by 7 binary digits each (2^7 = 128).
		// We can optionally discretize the binary bytes, but this is not done now.
		double A1 = Math.floor(K1[0] * 10.0) / 10.0;
		double A0 = Math.floor(K1[1] * 10.0) / 10.0;
		double B1 = Math.floor(K1[2] * 10.0) / 10.0;
		double B0 = Math.floor(K1[3] * 10.0) / 10.0;
		double carryFlag = K1[4];
		double currentDigit = K1[5];
		double C1 = K1[6];
		double C0 = K1[7];
		double resultReady = K1[8];
		double underflowError = K1[9];
		double C0b = K1[10];

		// System.out.println("A1,A0 = " + A1.toString() + A0.toString());
		// System.out.println("B1,B0 = " + B1.toString() + B0.toString());

		if (currentDigit < 0.5)
			{
			C0 = A0 - B0;
			if (A0 >= B0)                // C seems to support >= for comparison of doubles
				{
				C0b = C0;
				carryFlag = 0.0;
				}
			else
				{
				C0b = 1.0 + C0;
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
			currentDigit = 1.0;         // optional
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
		K2[10] = C0b;
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
		double[] K1 = new double[11];
		double[] K2 = new double[11];
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
					double err2 = Math.abs(K2[10] - c0);
					if (err1 > 0.001)
						correct = false;
					if (err2 > 0.001)
						correct = false;
					}

				System.out.println(" answer = " + digit(c1) + digit(c0));
				System.out.println(" genifer = " + digit(K2[6]) + digit(K2[10]));

				if (correct)
					System.out.println
							("\33[32m*********** Correct \33[39m\n");
				else
					{
					System.out.println("\33[31mWrong!!!!!! ");
					System.out.println("underflow = " + Double.toString(K2[9]) + "\33[39m\n");
					// beep();
					}
				}
			else
				{
				// Copy output K vector to input, for the next iteration
				System.arraycopy(K2, 0, K1, 0, 11);
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

	// At this point we are able to generate training examples for the transition operator.
	// Now try to see if we can train an ANN to approximate this operator.
	// First we try to learn the transition operator as a simple multi-layer NN.
	// This should be very simple and back-prop (or r-prop) will do.

	public static void arithmetic_testB() throws IOException
		{
		Integer N = 1000;			// size of training set
		double Ks[][] = new double[N][7];
		double Ks_star[][] = new double[N][7];

		// create a neural network, without using a factory
		BasicNetwork network = new BasicNetwork();
		// network config = {7, 20, 20, 6}; // first = input layer, last = output layer
		network.addLayer(new BasicLayer(null, true, 7));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 15));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 10));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 10));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 10));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 10));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), false, 7));
		network.getStructure().finalizeStructure();
		network.reset();
		new ConsistentRandomizer(-1, 1, 500).randomize(network);
		// System.out.println(network.dumpWeights());

		Integer M = 100;
		Double[] errors1 = new Double[M];	// two arrays for recording errors
		Double[] errors2 = new Double[M];
		double sum_err1 = 0.0, sum_err2 = 0.0;	// sums of errors
		int tail = 0;			// index for cyclic arrays (last-in, first-out)

		for (int i = 0; i < M; ++i)			// clear errors to 0.0
			errors1[i] = errors2[i] = 0.0;

		// start_NN_plot();
		// start_W_plot();
		// start_K_plot();
		// start_output_plot();
		// plot_ideal();
		// print("Press 'Q' to quit\n\n");

		double[] K = new double[11];
		double[] K_star = new double[11];
		java.util.Random r = new java.util.Random();

		for (Integer i = 0; true; ++i)			// training epochs
			{
			System.out.print("[" + i.toString() + "] ");

			// Prepare training set
			for (int j = 0; j < N; ++j)
				{
				// Create random K vector (only 4 + 2 + 1 elements)
				for (int k = 0; k < 4; ++k)
					K[k] = Ks[j][k] = Math.floor(r.nextDouble() * 10.0) / 10.0;
				for (int k = 4; k < 6; ++k)
					K[k] = Ks[j][k] = r.nextBoolean() ? 1.0 : 0.0;
				K[6] = Ks[j][6] = Math.floor(r.nextDouble() * 10.0) / 10.0;

				transition(K, K_star);				// get ideal values

				for (int k = 4; k < 11; ++k)
					Ks_star[j][k - 4] = K_star[k];
				}

			// create training data
			MLDataSet trainingSet = new BasicMLDataSet(Ks, Ks_star);

			// set up trainer
			final Backpropagation train = new Backpropagation(network, trainingSet);
			train.fixFlatSpot(false);

			// train the network!
			train.iteration();

			// Difference between actual outcome and desired value:
			Double training_err = train.getError();
			System.out.format(" Error = %.10f\n", training_err);

			/* Calculate mean error
			// Update error arrays cyclically
			// (This is easier to understand by referring to the next block of code)
			sum_err2 -= errors2[tail];
			sum_err2 += errors1[tail];
			sum_err1 -= errors1[tail];
			sum_err1 += training_err;
			// print("sum1, sum2 = %lf %lf\n", sum_err1, sum_err2);

			Double mean_err = (i < M) ? (sum_err1 / i) : (sum_err1 / M);
			System.out.println("mean abs error = " + mean_err.toString());

			// record new error in cyclic arrays
			errors2[tail] = errors1[tail];
			errors1[tail] = training_err;
			++tail;
			if (tail == M)				// loop back in cycle
				tail = 0;

			Double ratio = (sum_err2 - sum_err1) / sum_err1;
			if (ratio > 0)
				System.out.println("error ratio = " + ratio.toString());
			else
				System.out.println("error ratio = \33[31m" + ratio.toString() + "\33[39m");
			*/

			/* test the neural network
			System.out.println("Neural Network Results:");
			for (MLDataPair pair : trainingSet)
				{
				final MLData output = network.compute(pair.getInput());
				System.out.println(pair.getInput().getData(0) + "," + pair.getInput().getData(1)
						+ ", actual=" + output.getData(0) + ",ideal=" + pair.getIdeal().getData(0));
				}
			*/

			/* Testing set
			Double test_err = 0.0;
			for (int j = 0; j < 10; ++j)
				{
				// Create random K vector (only 4 + 2 elements)
				for (int k = 0; k < 4; ++k)
					K[k] = Math.floor(r.nextDouble() * 10.0) / 10.0;
				for (int k = 4; k < 6; ++k)
					K[k] = r.nextBoolean() ? 1.0 : 0.0;
				K[6] = Math.floor(r.nextDouble() * 10.0) / 10.0;

				forward_prop(Net, 6, K);		// input vector dimension = 6

				// Desired value = K_star
				transition(K, K_star);

				Double single_err = 0.0;
				for (int k = 4; k < 10; ++k)
					{
					double error = K_star[k] - LastLayer.neurons[k - 4].output;
					LastLayer.neurons[k - 4].error = error;		// record this for back-prop

					single_err += Math.abs(error);		// record sum of errors
					}
				test_err += single_err;
				}
			test_err /= 10.0;
			System.out.print("random test error = " + test_err.toString());
			*/

			if ((i % 5000) == 0)			// display status periodically
				{
				// plot_output(Net);
				// flush_output();
				// plot_W(Net);
				// plot_NN(Net);
				// plot_trainer(0);		// required to clear the window
				// plot_K();
				// if (quit = delay_vis(0))
					// break;
				}

			// if (isnan(ratio))
			//	break;
			// if (ratio - 0.5f < 0.0000001)	// ratio == 0.5 means stationary
			// if (test_err < 0.01)
			if (training_err < 0.001)
				break;
			}

		System.out.print("Training finished\n\n");
		beep();
		// plot_output(Net);
		// flush_output();
		// plot_W(Net);

		//saveNet(Net, NumLayers, neuronsOfLayer);
		EncogDirectoryPersistence.saveObject(new File("testing.eg"), network);
		Encog.getInstance().shutdown();
		}

	public static int arithmetic_testC_1(BasicNetwork network)
		{
		double[] K1 = new double[11];
		double[] K2 = new double[11];
		double a1, a0, b1, b0;
		int ans = 0;

		java.util.Random r = new java.util.Random();
		// generate A,B randomly
		a1 = Math.floor(r.nextDouble() * 10.0) / 10.0;
		K1[0] = a1;
		a0 = Math.floor(r.nextDouble() * 10.0) / 10.0;
		K1[1] = a0;
		b1 = Math.floor(r.nextDouble() * 10.0) / 10.0;
		K1[2] = b1;
		b0 = Math.floor(r.nextDouble() * 10.0) / 10.0;
		K1[3] = b0;

		System.out.format("%c%c, %c%c: ", digit(a1), digit(a0), digit(b1), digit(b0));

		// correct answer
		Integer a = (int) (a1 * 10.0) * 10 + (int) (a0 * 10.0);
		Integer b = (int) (b1 * 10.0) * 10 + (int) (b0 * 10.0);
		System.out.format("%d - ", a);
		System.out.format("%d = ", b);

		int c = a - b;
		double c1 = (double)(c / 10) / 10.0;
		double c0 = (double)(c % 10) / 10.0;
		System.out.format("%d\n", c);

		//	double carryFlag = rand() / (double) RAND_MAX;
		//	K1[4] = carryFlag;
		//	double currentDigit = rand() / (double) RAND_MAX;
		//	K1[5] = currentDigit;

		K1[4] = 0.0;				// carry flag
		K1[5] = 0.0;				// current digit (0 or 1)
		K1[6] = 0.0;				// C1 (note that C1 is part of the input also)
		K1[7] = 0.0;				// C0 (but C0 is not an input)
		K1[8] = 0.0;				// result ready flag
		K1[9] = 0.0;				// underflow flag
		K1[10] = 0.0;				// C0b

		for (int loop = 0; loop < 2; ++loop)
			{
			// call the transition operator
			double[] in_array = Arrays.copyOfRange(K1, 0, 7);	// end-index = exclusive!
			MLData input = new BasicMLData(in_array);
			final MLData output = network.compute(input);
			for (int j = 0; j < 7; j++)
				System.out.format("%f ", output.getData(j));
			System.out.println();
			// input.getData(0), input.getData(1), ...
			// output.getData(0), ...

			for (int k = 4; k < 11; ++k)    // 4..10 = output vector
				K2[k] = output.getData(k - 4);

			// get result
			if (K2[8] > 0.5)          		// result ready?
				{
				double err1 = 0.0, err2 = 0.0;
				boolean correct = true;
				if (c < 0)                	// answer is negative
					{
					if (K2[9] < 0.5)    	// underflow is clear but should be set
						correct = false;
					}
				else
					{
					if (K2[9] >= 0.5)    	// underflow is set but should be clear
						correct = false;

					err1 = Math.abs(K2[6] - c1);
					err2 = Math.abs(K2[10] - c0);
					System.out.format(" err1, err2 = %f, %f\n", err1, err2);
					if (err1 > 0.099999)
						correct = false;
					if (err2 > 0.099999)
						correct = false;
					}

				System.out.format(" answer = %c%c   ", digit(c1), digit(c0));
				System.out.format(" genifer = %c%c\n", digit(K2[6]), digit(K2[10]));
				// System.out.print(" C1*,C0* = %f, %f   ", c1, c0);
				// System.out.print(" C1,C0 = %f, %f\n", K2[6], K2[7]);

				if (correct && c > 0)
					{
					ans = 1;
					System.out.print("\33[32m********* Yes!!!!\33[39m\n");
					}
				else if (correct)
					{
					ans = 2;
					System.out.print("\33[34mNegative YES \33[39m\n");
					}
				else
					{
					ans = 3;
					System.out.print("\33[31mWrong!!!! ");
					if (c < 0)
						System.out.format(" underflow = %f \33[39m\n", K2[9]);
					else
						System.out.format("\33[35m err1, err2 = %f, %f \33[39m\n", err1, err2);

					// beep();
					}
				}
			else
				{
				for (int k = 4; k < 11; ++k)
					K1[k] = K2[k];

				if (loop >= 1)
					{
					ans = 4;
					System.out.format("\33[31mNon-termination: ");
					System.out.format("result ready = %f \33[39m\n", K2[8]);
					// beep();
					}
				}
			}
		return ans;
		}

	public static void arithmetic_testC() throws IOException, ClassNotFoundException
		{
		BasicNetwork network = (BasicNetwork) EncogDirectoryPersistence
				.loadObject(new File("testing.eg"));

		int ans_correct = 0, ans_negative = 0, ans_wrong = 0, ans_non_term = 0;
		Integer P = 100;
		for (int i = 0; i < P; ++i)
			{
			System.out.format("(%d) ", i);
			switch (arithmetic_testC_1(network))
				{
				case 1:
					++ans_correct;
					break;
				case 2:
					++ans_negative;
					break;
				case 3:
					++ans_wrong;
					break;
				case 4:
					++ans_non_term;
					break;
				default:
					System.out.print("Answer error!\n");
					break;
				}
			}

		System.out.print("\n=======================\n");
		System.out.format("Answers correct  = %d (%.1f%%)\n", ans_correct, ans_correct * 100 / (float) P);
		System.out.format("Answers negative = %d (%.1f%%)\n", ans_negative, ans_negative * 100 / (float) P);
		System.out.format("Answers wrong    = %d (%.1f%%)\n", ans_wrong, ans_wrong * 100 / (float) P);
		System.out.format("Answers non-term = %d (%.1f%%)\n", ans_non_term, ans_non_term * 100 / (float) P);

		Encog.getInstance().shutdown();
		}

	/***

	// The input necessary for XOR
	public static double XOR_INPUT[][] = {{1.0, 0.0}, {0.0, 0.0},
			{0.0, 1.0}, {1.0, 1.0}};

	// The ideal data necessary for XOR
	public static double XOR_IDEAL[][] = {{1.0}, {0.0}, {1.0}, {0.0}};

	public static void simple_xor_test()
		{
		// create a neural network, without using a factory
		BasicNetwork network = new BasicNetwork();
		network.addLayer(new BasicLayer(null, true, 2));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 2));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), false, 1));
		network.getStructure().finalizeStructure();
		network.reset();
		new ConsistentRandomizer(-1, 1, 500).randomize(network);
		System.out.println(network.dumpWeights());

		// create training data
		MLDataSet trainingSet = new BasicMLDataSet(XOR_INPUT, XOR_IDEAL);

		// train the neural network
		final Backpropagation train = new Backpropagation(network, trainingSet, 0.7, 0.3);
		train.fixFlatSpot(false);

		int epoch = 1;

		do
			{
			train.iteration();
			System.out
					.println("Epoch #" + epoch + " Error:" + train.getError());
			epoch++;
			} while (train.getError() > 0.01);

		// test the neural network
		System.out.println("Neural Network Results:");
		for (MLDataPair pair : trainingSet)
			{
			final MLData output = network.compute(pair.getInput());
			System.out.println(pair.getInput().getData(0) + "," + pair.getInput().getData(1)
					+ ", actual=" + output.getData(0) + ",ideal=" + pair.getIdeal().getData(0));
			}

		Encog.getInstance().shutdown();
		}
	***/

	}
