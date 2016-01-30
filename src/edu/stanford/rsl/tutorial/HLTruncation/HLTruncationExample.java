package edu.stanford.rsl.tutorial.HLTruncation;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.tutorial.parallel.ParallelProjector2D;
import edu.stanford.rsl.tutorial.phantoms.Ellipsoid;
import edu.stanford.rsl.tutorial.phantoms.SheppLogan;

public class HLTruncationExample {
	public static void main(String [] args) {
		//phantoms
		Ellipsoid elli = new Ellipsoid(200,150);
		SheppLogan phantom = new SheppLogan(200, false);
		///
		ParallelProjector2D proj = new ParallelProjector2D(Math.PI,Math.PI/360,110,0.4);
		Grid2D sino = proj.projectRayDriven(elli);//oder elli
		sino.show("sinogram");
		
		HLTruncation truncationFix = new HLTruncation(35000);
		Grid2D corrSino = truncationFix.CorrectTruncation(sino);
		Grid2D extendedSino = truncationFix.extendedSino(sino);
		extendedSino.show("extended sino");
		corrSino.show("corrected sinogram");
		
	}
	
	
}
