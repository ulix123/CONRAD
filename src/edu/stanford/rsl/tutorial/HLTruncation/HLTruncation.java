package edu.stanford.rsl.tutorial.HLTruncation;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;

public class HLTruncation {
	private float truncThreshold;
	private int sinogramExtensionSize;
	
	public HLTruncation(float truncThreshold) {
		this.truncThreshold = truncThreshold;
		
	}
	
	public Grid2D CorrectTruncation(Grid2D sinogram) {
		float[] massPerProjection = getMassPerProjection(sinogram);
		boolean[] truncatedProjections = getTruncatedProjections(massPerProjection);
		float meanMassNonTruncProjections = getMeanMassNonTruncProjections(massPerProjection,truncatedProjections);
		float[][] WidthTruncationCorrection = getWidthTruncationCorrection(sinogram,massPerProjection,truncatedProjections,meanMassNonTruncProjections);
		Grid2D extendedSinogram = extendedSinogram(sinogram);
		Grid2D fixSino = extrapolateTruncation(extendedSinogram,truncatedProjections,WidthTruncationCorrection);
		return fixSino;
	}
	public Grid2D extendedSino(Grid2D sinogram){
		float[] massPerProjection = getMassPerProjection(sinogram);
		boolean[] truncatedProjections = getTruncatedProjections(massPerProjection);
		float meanMassNonTruncProjections = getMeanMassNonTruncProjections(massPerProjection,truncatedProjections);
		//float[][] WidthTruncationCorrection = getWidthTruncationCorrection(sinogram,massPerProjection,truncatedProjections,meanMassNonTruncProjections);
		Grid2D extendedSinogram = extendedSinogram(sinogram);
		return extendedSinogram;
	}

	public float[] getMassPerProjection( Grid2D sinogram){
		int nProjections = sinogram.getHeight();
		int nDetectorPixels = sinogram.getWidth();
		float[] massPerProjection = new float[nProjections];
		
		for(int cproj=0; cproj < nProjections; cproj++){
			massPerProjection[cproj] = 0;
			for(int detectorPixels = 0; detectorPixels < nDetectorPixels; detectorPixels++){
				massPerProjection[cproj] += sinogram.getPixelValue(detectorPixels,cproj);
			}
		}
		return massPerProjection;		
	}
	
	private boolean[] getTruncatedProjections(float[] massPerProjection){
		
		boolean[] truncatedProjections = new boolean[massPerProjection.length];
		
		for(int cproj = 0; cproj < massPerProjection.length; cproj++){
			if(massPerProjection[cproj] < truncThreshold){
				truncatedProjections[cproj] = true;
			}else{
				truncatedProjections[cproj] = false;
			}	
		}
		return truncatedProjections;	
	}
	 
	private float getMeanMassNonTruncProjections(float[] massPerProjections,boolean[] truncatedProjections){
		float sumNonTruncMass = 0.f;
		int countNonTruncatedProjections = 0;
		
		for(int cproj = 0; cproj < massPerProjections.length; cproj++){
			if (!truncatedProjections[cproj]){
				sumNonTruncMass += massPerProjections[cproj];
				countNonTruncatedProjections++;
			}
		}
		return sumNonTruncMass/countNonTruncatedProjections;
	}
	
	private float[][] getWidthTruncationCorrection(Grid2D sinogram, float[] massPerProjections,boolean[] truncatedProjections,float meanMassNonTruncProjections){
		
		float[][] WidthTruncCorr = new float[massPerProjections.length][2];
		float maxWidth = 0.f;
				
		for(int cproj = 0; cproj < massPerProjections.length; cproj++){
			if(truncatedProjections[cproj]){
				float MassDiff = meanMassNonTruncProjections - massPerProjections[cproj];
				float greyValueLeft = sinogram.getPixelValue(0,cproj);
				float greyValueRight = sinogram.getPixelValue(sinogram.getWidth()-1, cproj);
				float massLeft = MassDiff*(1-1/(1+greyValueLeft/greyValueRight));
				float massRight = MassDiff - massLeft;
				//links 0 rechts 1
				WidthTruncCorr[cproj][0] = 2*massLeft/greyValueLeft;
				WidthTruncCorr[cproj][1] = 2*massRight/greyValueRight;
				if(WidthTruncCorr[cproj][0] > maxWidth){
					maxWidth = WidthTruncCorr[cproj][0];
				}
				if(WidthTruncCorr[cproj][1] > maxWidth){
					maxWidth = WidthTruncCorr[cproj][1];
				}
			}else{
				WidthTruncCorr[cproj][0] = 0;
				WidthTruncCorr[cproj][1] = 0;
			}
		}
		sinogramExtensionSize = (int)maxWidth +1;
		return WidthTruncCorr;	
	}
	
	private Grid2D extendedSinogram(Grid2D sinogram){
		Grid2D extendedSinogram = new Grid2D(sinogram.getWidth()+2*sinogramExtensionSize,sinogram.getHeight());
		
		for(int x = 0; x < extendedSinogram.getWidth(); x++){
			for(int y = 0; y < extendedSinogram.getHeight(); y++){
				if( x < sinogramExtensionSize || x >= extendedSinogram.getWidth() - sinogramExtensionSize){
					extendedSinogram.putPixelValue(x, y, 0);		
				}else{
					extendedSinogram.putPixelValue(x, y, sinogram.getPixelValue(x-sinogramExtensionSize, y));
				}
			}
		}
		return extendedSinogram;
	}
	
	private Grid2D extrapolateTruncation(Grid2D extendedSinogram, boolean[] truncatedProjections, float[][] WidthTruncCorr){
		
		for(int cproj = 0; cproj < truncatedProjections.length;cproj++){
			if(truncatedProjections[cproj]){
				float greyValueLeft = extendedSinogram.getPixelValue(sinogramExtensionSize+1,cproj);
				float greyValueRight = extendedSinogram.getPixelValue(extendedSinogram.getWidth()- sinogramExtensionSize-1,cproj);
				float slopeLeft = greyValueLeft/WidthTruncCorr[cproj][0];
				float slopeRight = greyValueRight/WidthTruncCorr[cproj][1];
				//vom linken Bildrand bis eins vor dem linken Punkt
				for(int i = 0; i < (int)WidthTruncCorr[cproj][0];i++){
					int leftIdx = (int) (sinogramExtensionSize+1 - WidthTruncCorr[cproj][0])+i;
					float cVal = i*slopeLeft;
					extendedSinogram.putPixelValue(leftIdx, cproj, cVal);
				}	
				//von rechtem i+1 punkt zum rechten Bildrand
				for(int j = 0; j < (int)WidthTruncCorr[cproj][1];j++){	
					int rightIdx = (int) (extendedSinogram.getWidth() - sinogramExtensionSize+j);
					float cVal = greyValueRight - (j+1) * slopeRight;
					extendedSinogram.putPixelValue(rightIdx, cproj, cVal);
				}
			}
		}
		return extendedSinogram;
	}
}
