package edu.stanford.rsl.tutorial.HLTruncation;

import ij.IJ;
import ij.ImageJ;
import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.tutorial.parallel.ParallelProjector2D;
import edu.stanford.rsl.tutorial.phantoms.Ellipsoid;
import edu.stanford.rsl.tutorial.phantoms.SheppLogan;

public class HLTruncationExample {
	
	public static void main(String[] arg) {
		new ImageJ();
		SheppLogan sebbi = new SheppLogan(200, false);
		Ellipsoid elli = new Ellipsoid(200, 150);
		ParallelProjector2D proj = new ParallelProjector2D(Math.PI, Math.PI/360.0, 165, 0.4); //165
		Grid2D sinosebbi = proj.projectRayDriven(sebbi);
		proj = new ParallelProjector2D(Math.PI, Math.PI/360.0, 120, 0.4);
		Grid2D sinoelli = proj.projectRayDriven(elli);
		sinosebbi.show("Sinogram Shepp-Logan");
		sinoelli.show("Sinogram ellipsodes");
		HLTruncation hl = new HLTruncation(12260); //SheppLogan parameter
		Grid2D corrSinoSebbi = hl.correctTruncation(sinosebbi);
		hl.setTruncationThreshold(35100);
		Grid2D corrSinoElli = hl.correctTruncation(sinoelli);
		corrSinoSebbi.show("Corrected sinogram Shepp-Logan");
		corrSinoElli.show("Corrected sinogram ellipsoids");
		
	}
	
	
	

}
