/*******************************************************************************
AccCostAlgorithm.java
Copyright (C) Victor Olaya

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*******************************************************************************/
package es.udc.cartolab.gvsig.algs.condcost;

import java.util.ArrayList;

import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.rasterWrappers.GridCell;
import es.unex.sextante.rasterWrappers.GridExtent;

public class AccCostAlgorithm extends GeoAlgorithm{

	public static final String COST = "COST";
	public static final String FEATURES = "FEATURES";
	//public static final String THRESHOLD = "THRESHOLD";
	public static final String ACCCOST = "ACCCOST";
	public static final String CLOSESTPOINT = "CLOSESTPOINT";
	public static final String DISTANCE = "DISTANCE";

	private static final int NO_DATA = -1;

	public static final int EUCLIDEAN = 0;
	public static final int CHESSBOARD = 1;
	public static final int MANHATTAN = 2;
	public static final int CHAMFER = 3;
	public static final int WINDOW5X5 = 4;

	private int m_iNX, m_iNY;
	private int m_iDistance;
	//private double m_dThreshold;
	private IRasterLayer m_Cost;
	private IRasterLayer m_Features;
	private IRasterLayer m_AccCost;
	private IRasterLayer m_ClosestPoint;
	private ArrayList m_CentralPoints, m_AdjPoints;

	public void defineCharacteristics() {

		String [] sOptions = {Sextante.getText( "Euclidean"),
								Sextante.getText( "Chessboard"),
								Sextante.getText( "Manhattan"),
								Sextante.getText( "Chamfer 3:4"),
								" 5 X 5"};
		setName(Sextante.getText("Accumulated_cost__isotropic"));
		setGroup(Sextante.getText("Cost_distances_and_routes"));
		setGeneratesUserDefinedRasterOutput(true);
		setIsDeterminatedProcess(false);

		try {
			m_Parameters.addInputRasterLayer(COST,
											Sextante.getText("Unitary_cost"),
											true);
			m_Parameters.addInputRasterLayer(FEATURES,
											Sextante.getText("Origin-destination_points"),
											true);
			/*m_Parameters.addNumericalValue(THRESHOLD,
											Sextante.getText("Threshold_for_route_separation"),
											0,
											AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);*/
			addOutputRasterLayer(ACCCOST, Sextante.getText("Accumulated_cost"));
			addOutputRasterLayer(CLOSESTPOINT, Sextante.getText("Closest_points"));
			m_Parameters.addSelection(DISTANCE,
											Sextante.getText( "Type_of_distance"),
											sOptions);

		} catch (RepeatedParameterNameException e) {
			Sextante.addErrorToLog(e);
		}

	}

	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

		int x,y;
		int iPoint = 1;
		double dValue;

		m_CentralPoints = new ArrayList();
		m_AdjPoints = new ArrayList();

		m_Cost = m_Parameters.getParameterValueAsRasterLayer(COST);
		m_Features = m_Parameters.getParameterValueAsRasterLayer(FEATURES);
		m_iDistance = m_Parameters.getParameterValueAsInt(DISTANCE);

		//m_dThreshold = m_Parameters.getParameterValueAsDouble(THRESHOLD);

		m_AccCost = getNewRasterLayer(ACCCOST,
				Sextante.getText("Accumulated_cost"),
				IRasterLayer.RASTER_DATA_TYPE_DOUBLE);
		m_ClosestPoint = getNewRasterLayer(CLOSESTPOINT,
				Sextante.getText("Closest_points"),
				IRasterLayer.RASTER_DATA_TYPE_INT);

		GridExtent extent = m_AccCost.getWindowGridExtent();

		m_Cost.setWindowExtent(extent);
		m_Cost.setInterpolationMethod(IRasterLayer.INTERPOLATION_BSpline);

		m_Features.setWindowExtent(extent);
		m_Features.setInterpolationMethod(IRasterLayer.INTERPOLATION_NearestNeighbour);

		m_iNX = m_Cost.getNX();
		m_iNY = m_Cost.getNY();

		m_AccCost.setNoDataValue(NO_DATA);
		m_AccCost.assignNoData();

		m_ClosestPoint.setNoDataValue(NO_DATA);
		m_ClosestPoint.assignNoData();

		for(y=0; y<m_iNY; y++){
			for(x=0; x<m_iNX; x++){
				dValue = m_Features.getCellValueAsDouble(x, y);
				if (dValue != 0.0 && !m_Features.isNoDataValue(dValue)){
					m_CentralPoints.add(new GridCell(x,y,iPoint));
					m_AccCost.setCellValue(x,y,0.0);
					m_ClosestPoint.setCellValue(x,y,iPoint);
					iPoint++;
				}
			}
		}

		if (m_iDistance != WINDOW5X5){
			calculateCost3X3();
		}
		else{
			calculateCost5X5();
		}

		return !m_Task.isCanceled();

	}

	private void calculateCost5X5() {

		int i, j;
		int iPt;
	    int iPoint;
		int x,y,x2,y2;
		double dAccCost;
		double dCost;
		double dPrevAccCost;
		GridCell cell;

		double dDist[][] = new double [5][5];

		for (i = -2; i < 3; i++) {
			for (j = -1; j < 2; j++) {
				dDist[i+2][j+2] = Math.sqrt(i*i+j*j);
			}
		}


		while (m_CentralPoints.size()!=0 && !m_Task.isCanceled()){
			for (iPt=0; iPt<m_CentralPoints.size();iPt++){
				cell = (GridCell) m_CentralPoints.get(iPt);
				x = cell.getX();
				y = cell.getY();
				iPoint = (int) cell.getValue();
				for (i = -2; i < 3; i++) {
					for (j = -2; j < 3; j++) {
						x2 = x + i;
						y2 = y + j;
						dCost = getCostTo(x,y, i, j);
						if (dCost != NO_DATA){
							dAccCost = m_AccCost.getCellValueAsDouble(x, y);
							dAccCost += (dCost * dDist[i+2][j+2]);
							dPrevAccCost = m_AccCost.getCellValueAsDouble(x2,y2);
							if (m_AccCost.isNoDataValue(dPrevAccCost) ||
									dPrevAccCost > dAccCost){
								m_AccCost.setCellValue(x2, y2, dAccCost);
								m_ClosestPoint.setCellValue(x2, y2, iPoint);
								m_AdjPoints.add(new GridCell(x2, y2, iPoint));
							}
						}
					}
				}
			}

			m_CentralPoints = m_AdjPoints;
			m_AdjPoints = new ArrayList();
		}

	}

	private double getCostTo(int x, int y, int i, int j) {

		int n, nMax;
		int iCells = 0;
		double di, dj;
		double dCost = 0;
		double dPartialCost;

		if (i == 0 && j == 0){
			return 0;
		}

		if (i > j){
			dj = Math.abs((double)j/(double)i) * Math.signum(j);
			di = Math.signum(i);
			nMax = Math.abs(i);
		}
		else{
			di = Math.abs((double)i/(double)j) * Math.signum(i);
			dj = Math.signum(j);
			nMax = Math.abs(j);
		}

		double ii = 0;
		double jj = 0;
		for (n = 0; n <= nMax; n++, ii+=di, jj+=dj) {
			dPartialCost = m_Cost.getCellValueAsDouble((int)(x + ii), (int)(y + jj));
			if (m_Cost.isNoDataValue(dPartialCost) || dPartialCost <= 0){
				return NO_DATA;
			}
			else{
				dCost += dPartialCost;
				iCells++;
			}
		}

		return dCost / (double) iCells;

	}

	private void calculateCost3X3(){

		int i, j;
		int iPt;
	    int iPoint;
		int x,y,x2,y2;
		double dAccCost;
		double dCost1, dCost2;
		double dPrevAccCost;
		GridCell cell;

		double dDist[][] = new double [3][3];

		switch (m_iDistance){
		case EUCLIDEAN:
		default:
			for (i = -1; i < 2; i++) {
				for (j = -1; j < 2; j++) {
					dDist[i+1][j+1] = Math.sqrt(i*i+j*j);
				}
			}
			break;
		case CHESSBOARD:
			double chessboard[][] = {{1,1,1},{1,0,1},{1,1,1}};
			dDist = chessboard;
			break;
		case MANHATTAN:
			double manhattan[][] = {{2,1,2},{1,0,1},{2,1,2}};
			dDist = manhattan;
			break;
		case CHAMFER:
			double chamfer[][] = {{4,3,4},{3,0,3},{4,3,4}};
			dDist = chamfer;
			break;
		}

		while (m_CentralPoints.size()!=0 && !m_Task.isCanceled()){
			for (iPt=0; iPt<m_CentralPoints.size();iPt++){
				cell = (GridCell) m_CentralPoints.get(iPt);
				x = cell.getX();
				y = cell.getY();
				iPoint = (int) cell.getValue();
				dCost1 = m_Cost.getCellValueAsDouble(x, y);
				for (i = -1; i < 2; i++) {
					for (j = -1; j < 2; j++) {
						x2 = x + i;
						y2 = y + j;
						dCost2 = m_Cost.getCellValueAsDouble(x2,y2);
						if (!m_Cost.isNoDataValue(dCost1) && !m_Cost.isNoDataValue(dCost2)
								&& dCost1 > 0 && dCost2 > 0){
							dAccCost = m_AccCost.getCellValueAsDouble(x, y);
							dAccCost += ((dCost1 + dCost2) / 2.0 * dDist[i+1][j+1]);
							dPrevAccCost = m_AccCost.getCellValueAsDouble(x2,y2);
							if (m_AccCost.isNoDataValue(dPrevAccCost) ||
									dPrevAccCost > dAccCost){
								m_AccCost.setCellValue(x2, y2, dAccCost);
								m_ClosestPoint.setCellValue(x2, y2, iPoint);
								m_AdjPoints.add(new GridCell(x2, y2, iPoint));
							}
						}
					}
				}
			}

			m_CentralPoints = m_AdjPoints;
			m_AdjPoints = new ArrayList();
			setProgressText(Integer.toString(m_CentralPoints.size()));
		}
	}

}