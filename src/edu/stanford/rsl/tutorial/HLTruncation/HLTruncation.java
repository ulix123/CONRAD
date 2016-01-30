package edu.stanford.rsl.tutorial.HLTruncation;

import ij.IJ;

import java.util.Arrays;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;

public class HLTruncation {
	private float truncationThreshold;
	private int sinogramExtensionSize;
	
	
	public HLTruncation(float truncThreshold) {
		this.truncationThreshold = truncThreshold;
	}
	
	public Grid2D correctTruncation(Grid2D sinogram) {
		IJ.showStatus("Computing mean mass of non-truncated projections...");
		float[] massPerProjection = getMassPerProjection(sinogram);
		IJ.showStatus("Retrieving truncated projections...");
		boolean[] truncatedProjections = getTruncatedProjections(massPerProjection);
		float meanMass = getMeanMassNonTruncatedProjections(massPerProjection, truncatedProjections);
		System.out.println("Mean mass of non-truncated projections: "+Float.toString(meanMass));
		float[][] widthTruncationCorrection = getWidthTruncationCorrection(sinogram, meanMass, massPerProjection, truncatedProjections);
		/*for (int i=0; i<massPerProjection.length; i++) {
			System.out.println(Float.toString(widthTruncationCorrection[i][0])+"       "+Float.toString(widthTruncationCorrection[i][1]));
			
		}
		*/
		Grid2D extendedSinogram = extendSinogram(sinogram);
		extendedSinogram = extrapolateTruncation(extendedSinogram, widthTruncationCorrection, truncatedProjections);
		
		
		return extendedSinogram;
	}
	
	public float[] getMassPerProjection(Grid2D sinogram) {
		int nProjections = sinogram.getHeight();
		int nDetectorPixels = sinogram.getWidth();
		float[] massPerProjection = new float[nProjections];
		for (int cproj=0; cproj<nProjections; cproj++) {
			massPerProjection[cproj] = 0;
			for (int detectorPixel=0; detectorPixel<nDetectorPixels; detectorPixel++) {
				massPerProjection[cproj] += sinogram.getPixelValue(detectorPixel, cproj);
			}
		}
		return massPerProjection;
	}
	
	private boolean[] getTruncatedProjections(float[] massPerProjection) {
		boolean[] truncatedProjections = new boolean[massPerProjection.length];
		for (int cproj=0; cproj<massPerProjection.length; cproj++) {
			if (massPerProjection[cproj] < truncationThreshold) {
				truncatedProjections[cproj] = true;
			} else {
				truncatedProjections[cproj] = false;
			}
		}
		return truncatedProjections;
	}
	
	private float getMeanMassNonTruncatedProjections(float[] massPerProjection, boolean[] truncatedProjections) {
		float meanMass = 0f;
		int countNonTruncatedProjections = 0;
		for (int cproj=0; cproj<massPerProjection.length; cproj++) {
			if (!truncatedProjections[cproj]) {
				countNonTruncatedProjections++;
				meanMass += massPerProjection[cproj];
			}
		}
		meanMass = meanMass / countNonTruncatedProjections;
		return meanMass;
	}
	
	private float[][] getWidthTruncationCorrection(Grid2D sinogram, float meanMass, float[] massPerProjection, boolean[] truncatedProjections) {
		float[][] widthTruncCorr = new float[massPerProjection.length][2];
		float maxWidth = 0f;
		for (int cproj=0; cproj<massPerProjection.length; cproj++){
			if (truncatedProjections[cproj]) {
				float massDifference = meanMass - massPerProjection[cproj];
				float greyValueLeft = sinogram.getPixelValue(0, cproj);
				float greyValueRight = sinogram.getPixelValue(sinogram.getWidth()-1, cproj);
				float massLeft = massDifference * (1 - 1/(1+greyValueLeft/greyValueRight));
				float massRight = massDifference - massLeft;
				widthTruncCorr[cproj][0] = 2 * massLeft/greyValueLeft;
				widthTruncCorr[cproj][1] = 2 * massRight/greyValueRight;
				if (widthTruncCorr[cproj][0] > maxWidth) {
					maxWidth = widthTruncCorr[cproj][0];
				}
				if (widthTruncCorr[cproj][1] > maxWidth) {
					maxWidth = widthTruncCorr[cproj][1];
				}
				
			} else {
				widthTruncCorr[cproj][0] = 0;
				widthTruncCorr[cproj][1] = 0;
			}
		}
		sinogramExtensionSize = (int) maxWidth + 1;
		return widthTruncCorr;
	}
	
	private Grid2D extendSinogram(Grid2D sinogram) {
		Grid2D extendedSinogram = new Grid2D(sinogram.getWidth() + 2*sinogramExtensionSize, sinogram.getHeight());
		
		for (int x=0; x<extendedSinogram.getWidth(); x++) {
			for (int y=0; y<extendedSinogram.getHeight(); y++) {
				if (x < sinogramExtensionSize || x >= extendedSinogram.getWidth() - sinogramExtensionSize) {
					extendedSinogram.putPixelValue(x, y, 0);
				} else {
					extendedSinogram.putPixelValue(x, y, sinogram.getPixelValue(x - sinogramExtensionSize, y));
				}
			}
		}
		return extendedSinogram;
	}
	
	private Grid2D extrapolateTruncation(Grid2D extendedSinogram, float[][] widthTruncationCorrection, boolean[] truncatedProjections) {
		for (int cproj=0; cproj<truncatedProjections.length; cproj++) {
			IJ.showProgress(cproj, truncatedProjections.length);
			IJ.showStatus("Extrapolating truncated projections...");
			if (truncatedProjections[cproj]) {
				float greyValueLeft = extendedSinogram.getPixelValue(sinogramExtensionSize+1, cproj);
				float greyValueRight = extendedSinogram.getPixelValue(extendedSinogram.getWidth() - sinogramExtensionSize - 1, cproj);
				float slopeLeft = greyValueLeft / widthTruncationCorrection[cproj][0];
				float slopeRight = greyValueRight / widthTruncationCorrection[cproj][1];
				for (int i=0; i<(int)widthTruncationCorrection[cproj][0]; i++) {
					int leftIdx = sinogramExtensionSize - (int)widthTruncationCorrection[cproj][0] + i;
					float cVal = i * slopeLeft;
					extendedSinogram.putPixelValue(leftIdx, cproj, cVal);
				}
				for (int i=0; i<(int)widthTruncationCorrection[cproj][1]; i++) {
					int rightIdx = extendedSinogram.getWidth() - sinogramExtensionSize + i;
					float cVal = greyValueRight - (i+1) * slopeRight;
					extendedSinogram.putPixelValue(rightIdx, cproj, cVal);
				}
				
			}
			
		}
		
		return extendedSinogram;
	}
	
	public void setTruncationThreshold(float threshold) {
		this.truncationThreshold = threshold;
	}

}
