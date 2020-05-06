package comp533;

import gradingTools.comp533s20.assignment7.Assignment7Suite;
import trace.grader.basics.GraderBasicsTraceUtility;
import util.trace.Tracer;

public class RunS20A7Tests {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Tracer.showInfo(true);
		GraderBasicsTraceUtility.setBufferTracedMessages(false);
		Assignment7Suite.setProcessTimeOut(5);
		Tracer.setMaxTraces(8000);
		Assignment7Suite.main(args);
	}

}
