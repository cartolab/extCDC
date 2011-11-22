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
package es.udc.sextante.gridAnalysis.conditionalCost_alberto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import es.unex.sextante.additionalInfo.AdditionalInfoMultipleInput;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.dataObjects.ITable;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.IteratorException;
import es.unex.sextante.exceptions.NullParameterValueException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.exceptions.UnsupportedOutputChannelException;
import es.unex.sextante.exceptions.WrongParameterIDException;
import es.unex.sextante.exceptions.WrongParameterTypeException;
import es.unex.sextante.outputs.FileOutputChannel;
import es.unex.sextante.outputs.IOutputChannel;
import es.unex.sextante.rasterWrappers.GridCell;


/**
 * To compile this algorithm it must be place on gridAnalysis project of SEXTANTE
 * 
 * @author uve
 * 
 */
public class AlbertoConditionalCostAlgorithm
extends
GeoAlgorithm {

	private static final boolean DEBUG = true;
	
	public static final String            COST                           = "COST";
	public static final String            ORIG_DEST                      = "ORIG_DEST";
	public static final String            CONDITIONAL_COST_SURFACES      = "CONDITINAL_COST_SURFACES";
	public static final String            MOVEMENT_SURFACES              = "MOVEMENT_SURFACES";
	public static final String            MOVEMENT_SURFACES_GROUPS_TABLE = "MOVEMENT_SURFACES_GROUP";
	public static final String            MOVEMENT_CONSTRAINTS_TABLE     = "MOVEMENT_CONSTRAINTS";
	public static final String            DISTANCE                       = "DISTANCE";

	//public static final String THRESHOLD = "THRESHOLD";
	public static final String            OUTPUT_ACCCOST                 = "ACCCOST";
	public static final String            OUTPUT_CLOSESTPOINT            = "CLOSESTPOINT";
	public static final String            OUTPUT_CONDITIONAL_COSTS       = "COND_COST";

	private static final int              NO_DATA                        = -1;
	private static final int              ORG_DST_DATA                   = 0;

	public static final int               EUCLIDEAN                      = 0;
	public static final int               CHESSBOARD                     = 1;
	public static final int               MANHATTAN                      = 2;
	public static final int               CHAMFER                        = 3;
	public static final int               WINDOW5X5                      = 4;

	private int                           m_iNX, m_iNY;
	private int                           m_iDistance;
	//private double m_dThreshold;
	private IRasterLayer                  m_Cost;
	private IRasterLayer                  m_Orig_Dest;

	private IRasterLayer                  m_Movement_Surfaces;
	private HashMap<String, IRasterLayer> input_Cond_Costs;

	private IRasterLayer                  output_GAccCost;
	private HashMap<String, IRasterLayer> output_Cond_AccCosts;

	private IRasterLayer                  m_ClosestPoint;
	private HashMap<String, IRasterLayer> m_Cond_ClosestPoint;

	private ArrayList                     m_CentralPoints, m_AdjPoints;
	private HashSet                       m_AdjPoints_Set;
	//   private HashMap<String, ArrayList>    m_Cond_CentralPoints, m_Cond_AdjPoints;

	HashMap<Integer, MovementSurface>     surfacesMap;


	@Override
	public void defineCharacteristics() {

		final String[] sOptions = { Sextante.getText("Euclidean"), Sextante.getText("Chessboard"), Sextante.getText("Manhattan"),
				Sextante.getText("Chamfer 3:4"), " 5 X 5" };
		setName(Sextante.getText("Alberto_Conditional_Cost"));
		setGroup(Sextante.getText("ALBERTO_ALG"));
		setUserCanDefineAnalysisExtent(true);
		//setIsDeterminatedProcess(false);

		try {
			m_Parameters.addInputRasterLayer(COST, Sextante.getText("Unitary_cost"), true);

			m_Parameters.addInputRasterLayer(ORIG_DEST, Sextante.getText("Origin-destination_cells"), true);

			m_Parameters.addInputRasterLayer(MOVEMENT_SURFACES, Sextante.getText("Movement_surfaces"), true);

			m_Parameters.addMultipleInput(CONDITIONAL_COST_SURFACES, Sextante.getText("Conditional_Cost_Surfaces"),
					AdditionalInfoMultipleInput.DATA_TYPE_RASTER, true);

			m_Parameters.addInputTable(MOVEMENT_SURFACES_GROUPS_TABLE, "Movement_Surfaces_Groups", true);

			m_Parameters.addInputTable(MOVEMENT_CONSTRAINTS_TABLE, "Movement_constraints", true);

			addOutputRasterLayer(OUTPUT_CONDITIONAL_COSTS, Sextante.getText("Conditional_Costs"));

			addOutputRasterLayer(OUTPUT_ACCCOST, Sextante.getText("Accumulated_cost"));
			// addOutputRasterLayer(ACCCOST, Sextante.getText("Accumulated_cost"));
			addOutputRasterLayer(OUTPUT_CLOSESTPOINT, Sextante.getText("Closest_points"));
			m_Parameters.addSelection(DISTANCE, Sextante.getText("Type_of_distance"), sOptions);

		}
		catch (final RepeatedParameterNameException e) {
			Sextante.addErrorToLog(e);
		}

	}

	/**
	 * surfaceMap must be initialized previously!!
	 * 
	 * @throws WrongParameterTypeException
	 * @throws WrongParameterIDException
	 * @throws NullParameterValueException
	 * @throws IteratorException
	 */
	private void initConstraints() throws WrongParameterTypeException, WrongParameterIDException, NullParameterValueException,
	IteratorException {

		final ITable movConstraintTable = m_Parameters.getParameterValueAsTable(MOVEMENT_CONSTRAINTS_TABLE);
		final int numConstraint = movConstraintTable.getFieldCount();

		final ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int i = 1; i < numConstraint; i++) {
			final int id = Integer.parseInt(movConstraintTable.getFieldName(i));
			ids.add(id);
		}

		//TODO Default should be more elegant!
		final HashMap<Integer, Boolean> defaultConstraintsMap = new HashMap<Integer, Boolean>();
		for (final Iterator<Integer> iter = ids.iterator(); iter.hasNext();) {
			final int id = iter.next();
			defaultConstraintsMap.put(id, true);
		}


		for (final IRecordsetIterator iter = movConstraintTable.iterator(); iter.hasNext();) {
			final IRecord values = iter.next();

			final HashMap<Integer, Boolean> constraintsMap = new HashMap<Integer, Boolean>();
			//TODO It should be a hashMap with surfacesID, not a Array
			final int surfID = Integer.parseInt(values.getValue(0).toString());
			for (final Integer id : ids) {
				final String v = values.getValue(id).toString();
				constraintsMap.put(id, isTrue(v));
			}
			final MovementSurface surface = surfacesMap.get(surfID);
			if (surface != null) {
				surface.setMovConstraints(constraintsMap);
			}
			else {
				//TODO
				System.out.println("ERROR: Any surface with ID ---------------> " + surfID);
			}
		}
	}


	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

		final ITable movSurfGroupsTable = m_Parameters.getParameterValueAsTable(MOVEMENT_SURFACES_GROUPS_TABLE);

		//TODO Create objects!!
		final ArrayList<IRasterLayer> costs_surfaces_array = m_Parameters.getParameterValueAsArrayList(CONDITIONAL_COST_SURFACES);

		//TODO Now It suppose that are ordered
		input_Cond_Costs = new HashMap<String, IRasterLayer>();
		output_Cond_AccCosts = new HashMap<String, IRasterLayer>();
		for (int i = 0; i < costs_surfaces_array.size(); i++) {
			final IRasterLayer cost_grid = costs_surfaces_array.get(i);
			input_Cond_Costs.put(cost_grid.getName(), cost_grid);
		}

		m_Movement_Surfaces = m_Parameters.getParameterValueAsRasterLayer(MOVEMENT_SURFACES);
		m_Movement_Surfaces.setFullExtent();

		m_Movement_Surfaces.open();

		final Object input_crs = m_Movement_Surfaces.getCRS();
		//final GridExtent input_extent = new GridExtent(m_Movement_Surfaces);

		try {
			m_Movement_Surfaces.postProcess();
		}
		catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		m_iNX = m_Movement_Surfaces.getNX();
		m_iNY = m_Movement_Surfaces.getNY();

		//COUNT DIFFERENT VALUES
		final HashSet<Integer> surfacesID = new HashSet<Integer>();
		final int counter = 0;
		for (int y = 0; y < m_iNY; y++) {
			for (int x = 0; x < m_iNX; x++) {
				final int value = m_Movement_Surfaces.getCellValueAsInt(x, y);
				//				if (value == ORG_DST_DATA){
				//					continue;
				//				}
				if (!m_Movement_Surfaces.isNoDataValue(value) && !surfacesID.contains(value)) {
					surfacesID.add(value);
				}
			}
		}

		//Init map of surfacesID and MovSurface
		initMovementSurfaceMap(movSurfGroupsTable);

		//Init map of surfacesID
		initConstraints();

		int x, y;
		int iPoint = 1;
		double dValue;

		m_CentralPoints = new ArrayList();

		m_Cost = m_Parameters.getParameterValueAsRasterLayer(COST);
		m_Orig_Dest = m_Parameters.getParameterValueAsRasterLayer(ORIG_DEST);
		m_iDistance = m_Parameters.getParameterValueAsInt(DISTANCE);

		//m_dThreshold = m_Parameters.getParameterValueAsDouble(THRESHOLD);

		output_GAccCost = getNewRasterLayer(OUTPUT_ACCCOST, Sextante.getText("Accumulated_cost"),
				IRasterLayer.RASTER_DATA_TYPE_DOUBLE);
		m_ClosestPoint = getNewRasterLayer(OUTPUT_CLOSESTPOINT, Sextante.getText("Closest_points"),
				IRasterLayer.RASTER_DATA_TYPE_INT);

		//final GridExtent extent = output_GAccCost.getWindowGridExtent();

		m_Cost.setWindowExtent(m_AnalysisExtent);
		m_Cost.setInterpolationMethod(IRasterLayer.INTERPOLATION_BSpline);

		m_Orig_Dest.setWindowExtent(m_AnalysisExtent);
		m_Orig_Dest.setInterpolationMethod(IRasterLayer.INTERPOLATION_NearestNeighbour);

		m_iNX = m_Cost.getNX();
		m_iNY = m_Cost.getNY();

		output_GAccCost.setNoDataValue(NO_DATA);
		output_GAccCost.assignNoData();

		m_ClosestPoint.setNoDataValue(NO_DATA);
		m_ClosestPoint.assignNoData();

		//TODO HOW TO SAVE IN A USER FOLDER!???!?!??!?!
		// TEST /////////////////////
		//final IRasterLayer[] result = new IRasterLayer[surfacesID.size()];
		//CREATING OUTPUT RASTERS
		int i = 0;
		for (final String k : input_Cond_Costs.keySet()) {
			i++;
			final String name = OUTPUT_CONDITIONAL_COSTS + Integer.toString(i);
			final IOutputChannel channel = getMyOutputChannel(String.valueOf(k));
			/////// REVISAR COMO SE USA AHORA LA EXTENT
			final IRasterLayer cac = m_OutputFactory.getNewRasterLayer(name, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, m_AnalysisExtent,
					1, channel, input_crs);

			cac.setWindowExtent(m_AnalysisExtent);
			cac.open();

			cac.setNoDataValue(NO_DATA);
			cac.assignNoData();

			//TODO Esto debe hacerse al final de todo al tener la capa ya creadita
			addOutputRasterLayer(name, name, 1, channel, cac);

			cac.close();
			output_Cond_AccCosts.put(k, cac);
		}
		
		//      ///////////////////////////////////
		//      final String name = OUTPUT_ACCCOST + Integer.toString(i);
		//      final IOutputChannel channel = getMyOutputChannel(name);
		//      addOutputRasterLayer(name, name, 1, channel, output_GAccCost);
		//      output_GAccCost.open();
		//      output_GAccCost.setNoDataValue(NO_DATA);
		//      output_GAccCost.assignNoData();
		//
		//      output_GAccCost.close();


		//INITIALIZING...
		for (y = 0; y < m_iNY; y++) {
			for (x = 0; x < m_iNX; x++) {
				dValue = m_Orig_Dest.getCellValueAsDouble(x, y);
				//if ((dValue != 0.0) && !m_Orig_Dest.isNoDataValue(dValue)) {
				if (!m_Orig_Dest.isNoDataValue(dValue)) {
					// Store the GridCell and of its CostSurface (-1 means Global Cost Surface)
					final Object[] ccs_cellValue = new Object[3];
					// "GLOBAL" if is simple... otherwise it is ccsID number
					ccs_cellValue[0] = "GLOBAL";
					ccs_cellValue[1] = new GridCell(x, y, iPoint);
					ccs_cellValue[2] = 0.0;
					m_CentralPoints.add(ccs_cellValue);
					output_GAccCost.setCellValue(x, y, 0.0);
					m_ClosestPoint.setCellValue(x, y, iPoint);
					//               for (final String k : m_Cond_CentralPoints.keySet()) {
					//                  final ArrayList centralPoints = m_Cond_CentralPoints.get(k);
					//                  centralPoints.add(new GridCell(x, y, iPoint));
					//               }
					for (final String k : output_Cond_AccCosts.keySet()) {
						final IRasterLayer out_acccost = output_Cond_AccCosts.get(k);
						//TODO Add on m_CentralPoints????
						out_acccost.setCellValue(x, y, 0.0);
					}
					//               for (final String k : m_Cond_ClosestPoint.keySet()) {
					//                  final IRasterLayer closestPoints = m_Cond_ClosestPoint.get(k);
					//                  closestPoints.setCellValue(x, y, iPoint);
					//               }

					iPoint++;
				}
			}
		}
		System.out.println("--------------------------------   m_CentralPoints.size(): " + m_CentralPoints.size());

		if (m_iDistance != WINDOW5X5) {
			calculateCost3X3();
		}
		//				else{
		//					calculateCost5X5();
		//				}

		return !m_Task.isCanceled();

	}


	private void initMovementSurfaceMap(ITable movSurfGroupsTable) throws IteratorException {
		HashMap<Integer, MovementSurface> movSurfaceMap = new HashMap<Integer, MovementSurface>();
		for (final IRecordsetIterator iter = movSurfGroupsTable.iterator(); iter.hasNext();) {
			final IRecord record = iter.next();
			//0 "Z_Class";
			//1" Z_Name";
			//2" CSS";
			//3" IS_NODE";
			final Object[] values = record.getValues();
			final int id_class = Integer.parseInt(values[0].toString());
			final String name = (String) values[1];
			final String css = (String) values[2];
			final boolean is_node = isTrue(values[3].toString());
			//TODO Number or Name of the GRID???
			//    	    final String cost_grid = values[4].toString().replaceAll("\"", "");
			//    	    System.out.println("COST_GRID: " + cost_grid);
			final IRasterLayer conditional_cost_grid = input_Cond_Costs.get(css);
			final MovementSurface ms = new MovementSurface(id_class, name, css, is_node, conditional_cost_grid);
			movSurfaceMap.put(id_class, ms);
		}
		surfacesMap = movSurfaceMap;
	}

	@SuppressWarnings("unchecked")
	private void calculateCost3X3() {

		int i, j;
		final int iPt;
		int iPoint;
		int x1, y1, x2, y2;
		double orgAccCost;
		double orgCostValue, dstCostValue;
		int orgSurfaceID, dstSurfaceID;
		double dPrevAccCost;
		GridCell cell;

		double dDist[][] = new double[3][3];
		double dist = 0;

		switch (m_iDistance) {
		//NOTE: Todo entre 2 para la semisuma
		case EUCLIDEAN:
		default:
			for (i = -1; i < 2; i++) {
				for (j = -1; j < 2; j++) {
					dDist[i + 1][j + 1] = Math.sqrt(i * i + j * j) * 0.5;
				}
			}
			break;
		case CHESSBOARD:
			final double chessboard[][] = { { 0.5, 0.5, 0.5 }, { 0.5, 0, 0.5 }, { 0.5, 0.5, 0.5 } };
			dDist = chessboard;
			break;
		case MANHATTAN:
			final double manhattan[][] = { { 1, 0.5, 1 }, { 0.5, 0, 0.5 }, { 1, 0.5, 1 } };
			dDist = manhattan;
			break;
		case CHAMFER:
			final double v_2_3 = (double) 3 / 2;
			final double chamfer[][] = { { 2, v_2_3, 2 }, { v_2_3, 0, v_2_3 }, { 2, v_2_3, 2 } };
			dDist = chamfer;
			break;
		}

		//INIT LAYERS
		for (int k = 0; k < surfacesMap.size(); k++) {
			final MovementSurface surface = surfacesMap.get(k);
			//System.out.println("Surface[" + k + "]: " + surface);
			if (surface == null) {
				continue;
			}
			surface.openAll();
		}

		int count = 0;
		m_AdjPoints = new ArrayList();
		m_AdjPoints_Set = new HashSet();
		while ((m_CentralPoints.size() != 0) && !m_Task.isCanceled()) {
			System.out.println(count++ + "--------------------------------   m_CentralPoints.size(): " + m_CentralPoints.size());
			//TODO Get MINOR VALUE on !!!!
			//for (iPt = 0; iPt < m_CentralPoints.size(); iPt++) {
			//            System.out.println(count++ + "------ m_CentralPoints.size(): " + m_CentralPoints.size() + "/" + iPt);
			//            System.out.println(count++ + "------ m_AdjPoints.size(): " + m_AdjPoints.size() + "/" + iPt);
			//            System.out.println();
			System.out.println();
			System.out.println();
			System.out.println("------------ m_CentralPoints.size(): " + m_CentralPoints.size());
			
			//NachoV: Ahora acabo de poner esta ordenacion antes del iter... ¿¿¿Es correcto???
			Collections.sort(m_CentralPoints, new XYGridcellValue_Comparator());
			final Iterator iter = m_CentralPoints.iterator();
			while (iter.hasNext()) {
				final Object[] ccsID_cellValue = (Object[]) iter.next();
				final String ccsID = (String) ccsID_cellValue[0];
				cell = (GridCell) ccsID_cellValue[1];
				final Double value = (Double) ccsID_cellValue[2];
				x1 = cell.getX();
				y1 = cell.getY();
				System.out.println("x,y: (" + x1 + ", " + y1 + ") " + ccsID + " -- " + value);
			}
			System.out.println();
			System.out.println();

			//final Object[] ccsID_GridCell_value = (Object[]) m_CentralPoints.get(iPt);
			final Object[] ccsID_cellValue = (Object[]) m_CentralPoints.get(0);
			m_CentralPoints.remove(0);
			String ccsID = (String) ccsID_cellValue[0];
			cell = (GridCell) ccsID_cellValue[1];
			final Double value = (Double) ccsID_cellValue[2];
			x1 = cell.getX();
			y1 = cell.getY();

			System.out.println("----> x,y: (" + x1 + ", " + y1 + ") " + ccsID + " -- " + value);

			iPoint = (int) cell.getValue();
			orgCostValue = m_Cost.getCellValueAsDouble(x1, y1);
			orgSurfaceID = -1;
			orgSurfaceID = m_Movement_Surfaces.getCellValueAsInt(x1, y1);

			for (i = -1; i < 2; i++) {
				for (j = -1; j < 2; j++) {
					x2 = x1 + i;
					y2 = y1 + j;
					if ((x1 == x2) && (y1 == y2)) {
						continue;
					}

					dist = dDist[i + 1][j + 1];
					dstCostValue = m_Cost.getCellValueAsDouble(x2, y2);
					dstSurfaceID = -1;
					dstSurfaceID = m_Movement_Surfaces.getCellValueAsInt(x2, y2);

					//TODO --------------------------------------------- Ahora se debe mirar los costes condicionales, no???
					final MovementSurface orgSurface = surfacesMap.get(orgSurfaceID);
					final MovementSurface dstSurface = surfacesMap.get(dstSurfaceID);

					//Check if there are cost cell with values on the conditinal grids
					final boolean orgHasValueInGAccCost = !output_GAccCost.isNoDataValue(output_GAccCost.getCellValueAsDouble(x1, y1));
					final boolean dstHasValueInGAccCost = !output_GAccCost.isNoDataValue(output_GAccCost.getCellValueAsDouble(x2, y2));
					//boolean dstHasConditionalCostValue = false;
					boolean dstHasSomeCondCostValue = false;
					boolean orgHasSomeCondCostValue = false;
					boolean orgHasValueInCondAccCost = false;
					final boolean dstHasValueInCondAccCost = false;

					boolean canMove = false;

					boolean someZDdependsOn_ORG = false;
					boolean someZDdependsOn_DST = false;
					MovementSurface surface = null;
					for (int k = 0; k < surfacesMap.size(); k++) {
						//TODO IMPORTANTE 
						// Y si tiene posibilidad de moverse a varios Superficies??? Como se itera???
						//final MovementSurface surface = surfacesMap.get(k);
						surface = surfacesMap.get(k);
						if ((surface == null)) {
							continue;
						}

						if (surface.hasConditionalCostValueAt(x1, y1)) {
							orgHasSomeCondCostValue = true;
						}

						if (surface.hasConditionalCostValueAt(x2, y2)) {
							dstHasSomeCondCostValue = true;
						}

					}

					canMove = canMoveTo(orgSurface, dstSurface.getID());

					// TODO Has a Map with the output_ConditionalAccCost layers
					if ((orgSurface != null) && orgSurface.hasConditionalCost()) {
						final IRasterLayer o_cac = output_Cond_AccCosts.get(orgSurface.getCCSName());
						orgHasValueInCondAccCost = orgSurface.isValidConditionalCost(o_cac.getValueAt(x1, y1));
					}

//					////////////////// Para parar en un punto conflictivo
//					if ((x1 == 5) && (y1 == 3) && (x2 == 5) && (y2 == 4)) {
//						//if ((orgSurfaceID == 5) && (dstSurfaceID == 3)) {
//						System.out.println("ZC_ORG: " + orgSurfaceID + "  X1, y1: " + x1 + "," + y1);
//						System.out.println("ZC_DST: " + dstSurfaceID + "  X2, y2: " + x2 + "," + y2);
//						//                     //                     System.out.println("PROBLEMS CONTINUE HERE!!!!!!!!!");
//					}


					//ALGORITHM CALCULUS
					if (!m_Cost.isNoDataValue(orgCostValue) && !m_Cost.isNoDataValue(dstCostValue) && (orgCostValue > 0)
							&& (dstCostValue > 0) && canMove(orgSurface, dstSurface, orgSurfaceID, dstSurfaceID)) {

						if (ccsID.equalsIgnoreCase("GLOBAL")) {
							//It means that the gridCell on m_CentralPoint belongs to Global Cost Surface
							if (dstHasSomeCondCostValue) {
								
								//TODO 
								if (orgSurface.isNode()){
									// orgZD is node of this ECD
									////////////////////////////////////
									if (DEBUG) {
										System.out.println("CACiD = CASo + d * ( CSo + CCiD)");
									}
									////////////////////////////////////
									ccsID = surface.getCCSName();
									IRasterLayer outputRaster = output_Cond_AccCosts.get(ccsID);
									double cost1 = orgCostValue;
									double cost2 = surface.getCCValueAt(x2, y2);
									orgAccCost = output_GAccCost.getCellValueAsDouble(x1, y1);
									dPrevAccCost = outputRaster.getCellValueAsDouble(x2, y2);
									setOutputValue(outputRaster, orgAccCost, dPrevAccCost, cost1, cost2, dist, x2, y2, ccsID, iPoint);
									
									// Preguntar a Alberto ZDo compatible ZDd es lo mismo que se puede mover???
									if (canMove){
										if (DEBUG) {
											System.out.println("CASd = CASo + d * ( CSo + CSd)");
										}
										////////////////////////////////////
										ccsID = surface.getCCSName();
										outputRaster = output_GAccCost;
										cost1 = orgCostValue;
										cost2 = dstCostValue;
										orgAccCost = output_GAccCost.getCellValueAsDouble(x1, y1);
										dPrevAccCost = outputRaster.getCellValueAsDouble(x2, y2);
										setOutputValue(outputRaster, orgAccCost, dPrevAccCost, cost1, cost2, dist, x2, y2, ccsID, iPoint);
									}
									
								}
							} else {
								
							}
						} else {
							//NO ES GLOBAL
						}

					}
					
					
//					//OLD ALGORITHM CALCULUS
//					if (!m_Cost.isNoDataValue(orgCostValue) && !m_Cost.isNoDataValue(dstCostValue) && (orgCostValue > 0)
//							&& (dstCostValue > 0) && canMove(orgSurface, dstSurface, orgSurfaceID, dstSurfaceID)) {
//
//						if (ccsID.equalsIgnoreCase("GLOBAL")) {
//							//It means that the gridCell on m_CentralPoint belongs to Global Cost Surface
//							if (dstHasSomeCondCostValue) {
//								if (someZDdependsOn_ORG) {
//									if (dstDependsOnOrg) {
//										final boolean dstHasConditionalCost = dstSurface.hasConditionalCost();
//										if (dstHasConditionalCost) {
//											for (int k = 0; k < surfacesMap.size(); k++) {
//												final MovementSurface surface = surfacesMap.get(k);
//												if ((surface == null)) {
//													continue;
//												}
//												if (surface.getID() != dstSurfaceID) {
//													continue;
//												}
//												final boolean orgHasCCValue = surface.hasConditionalCostValueAt(x1, y1);
//												//dstHasConditionalCostValue = surface.hasConditionalCostValueAt(x2, y2);
//												if (orgHasCCValue) {
//													//TODO
//													//CAC1
//													System.out.println("CAC1");
//													ccsID = surface.getCCSName();
//													final IRasterLayer outputRaster = output_Cond_AccCosts.get(ccsID);
//													final double cost1 = surface.getCCValueAt(x1, y1);
//													final double cost2 = surface.getCCValueAt(x2, y2);
//													orgAccCost = output_GAccCost.getCellValueAsDouble(x1, y1);
//													dPrevAccCost = outputRaster.getCellValueAsDouble(x2, y2);
//													setOutputValue(outputRaster, orgAccCost, dPrevAccCost, cost1, cost2, dist, x2, y2, ccsID,
//															iPoint);
//												}
//												else {
//													//TODO
//													//CAC2
//													System.out.println("CAC2");
//													ccsID = surface.getCCSName();
//													final IRasterLayer outputRaster = output_Cond_AccCosts.get(ccsID);
//													final double cost1 = m_Cost.getCellValueAsDouble(x1, y1);
//													final double cost2 = surface.getCCValueAt(x2, y2);
//													orgAccCost = output_GAccCost.getCellValueAsDouble(x1, y1);
//													dPrevAccCost = outputRaster.getCellValueAsDouble(x2, y2);
//													setOutputValue(outputRaster, orgAccCost, dPrevAccCost, cost1, cost2, dist, x2, y2, ccsID,
//															iPoint);
//												}
//											}
//										}
//									}
//									else { //NOT orgDependsOnDst
//										if (dstSurfaceID == orgSurfaceID) {
//											//TODO
//											//CAG1
//											setGlobalOutputValue(x1, y1, x2, y2, iPoint, dist);
//										}
//										else {
//											//Nothing
//										}
//
//									}
//								}
//								else { // NOT someZDdependsOn_ORG
//									//TODO
//									//CAG1
//									setGlobalOutputValue(x1, y1, x2, y2, iPoint, dist);
//								}
//							}
//							else { //NOT dstHasSomeConditionalCostValue
//								if (someZDdependsOn_DST) {
//									//                              for (int k = 0; k < surfacesMap.size(); k++) {
//									//                                 final MovementSurface surface = surfacesMap.get(k);
//									//                                 if ((surface == null)) {
//									//                                    continue;
//									//                                 }
//									if (orgHasSomeCondCostValue) {
//										//Nothing
//									}
//									else {
//										//TODO
//										//CAG1
//										setGlobalOutputValue(x1, y1, x2, y2, iPoint, dist);
//										//break; // orgHasConditionalValue on some CCS
//									}
//									//                              }
//								}
//								else {
//									//TODO
//									//CAG1
//									setGlobalOutputValue(x1, y1, x2, y2, iPoint, dist);
//								}
//							}
//						}
//						////////////////////////////////////////////////////////////
//						else {
//							//It means that the gridCell on m_CentralPoint belongs to Conditional Cost Surface
//
//							//TODO NO HACE FALTA AUX_SUR, NOF????
//							MovementSurface auxSurf = null;
//							for (final Integer surfID : surfacesMap.keySet()) {
//								auxSurf = surfacesMap.get(surfID);
//								if (auxSurf.getCCSName() == ccsID) {
//									break;
//								}
//							}
//							if (auxSurf == null) {
//								continue;
//							}
//							System.out.println("CC.name: " + auxSurf.getCCSName());
//							final boolean dstHasSCC = auxSurf.hasConditionalCostValueAt(x2, y2);
//							if (dstHasSCC) {
//								//TODO
//								//CAC3
//								System.out.println("CAC3");
//								ccsID = auxSurf.getCCSName();
//								final IRasterLayer outputRaster = output_Cond_AccCosts.get(ccsID);
//								final double cost1 = auxSurf.getCCValueAt(x1, y1);
//								//TODO rectificacion de alberto en ppt
//								//final double cost2 = m_Cost.getCellValueAsDouble(x2, y2);
//								final double cost2 = auxSurf.getCCValueAt(x2, y2);
//								orgAccCost = outputRaster.getCellValueAsDouble(x1, y1);
//								dPrevAccCost = outputRaster.getCellValueAsDouble(x2, y2);
//
//								setOutputValue(outputRaster, orgAccCost, dPrevAccCost, cost1, cost2, dist, x2, y2, ccsID, iPoint);
//							}
//							else {
//								//				if (orgSurface.dependsOn(dstSurfaceID)) {
//								//				    //TODO
//								//				    //CAG2
//								//				    System.out.println(">> CAG2 <<");
//								//				    final IRasterLayer ccsRaster = output_Cond_AccCosts.get(auxSurf.getCCSName());
//								//				    final double cost1 = auxSurf.getCCValueAt(x1, y1);
//								//				    final double cost2 = m_Cost.getCellValueAsDouble(x2, y2);
//								//				    orgAccCost = ccsRaster.getCellValueAsDouble(x1, y1);
//								//				    dPrevAccCost = output_GAccCost.getCellValueAsDouble(x2, y2);
//								//
//								//				    setOutputValue(output_GAccCost, orgAccCost, dPrevAccCost, cost1, cost2, dist, x2, y2, ccsID, iPoint);
//								//
//								//				}
//								//				else {
//								//				    //Nothing
//								//				}
//							}
//						}
//					}
				}
			}
		} // For para recorrer celdas
		//         m_CentralPoints = m_AdjPoints;
		//         m_AdjPoints = new ArrayList();
		m_AdjPoints_Set.clear();
		setProgressText(Integer.toString(m_CentralPoints.size()));
		//}

		//      //TODO CLOSE LAYERS
		//      for (int k = 0; k < surfacesMap.size(); k++) {
		//         final MovementSurface surface = surfacesMap.get(k);
		//         //System.out.println("Surface[" + k + "]: " + surface);
		//         if (surface == null) {
		//            continue;
		//         }
		//         surface.openAll();
		//      }

	}


	private void setGlobalOutputValue(final int x1,
			final int y1,
			final int x2,
			final int y2,
			final int iPoint,
			final double dist) {
		System.out.println(">> CAG1 <<   x2,y2: (" + x2 + ", " + y2 + ")");
		final double orgAccCost = output_GAccCost.getCellValueAsDouble(x1, y1);
		final double prevDstAccCost = output_GAccCost.getCellValueAsDouble(x2, y2);
		final double dCost1 = m_Cost.getCellValueAsDouble(x1, y1);
		final double dCost2 = m_Cost.getCellValueAsDouble(x2, y2);
		//dAccCost += ((dCost1 + dCost2) * dist);
		setOutputValue(output_GAccCost, orgAccCost, prevDstAccCost, dCost1, dCost2, dist, x2, y2, "GLOBAL", iPoint);
	}


	private void setOutputValue(final IRasterLayer outputLayer,
			final IRasterLayer orgAccCostLayer,
			final int x1,
			final int y1,
			final IRasterLayer orgLayer,
			final int x2,
			final int y2,
			final IRasterLayer dstLayer,
			final double dist,
			final String ccsID,
			final int iPoint) {

		double orgAccCost = orgAccCostLayer.getCellValueAsDouble(x2, y2);
		final double dCost1 = orgLayer.getCellValueAsDouble(x1, y1);
		final double dCost2 = dstLayer.getCellValueAsDouble(x2, y2);
		//TODO [NACHO] Se puede quitar el /2 en la matriz dDist y se ahorra computacion
		orgAccCost += ((dCost1 + dCost2) * dist);
		final double prevDstAccCost = outputLayer.getCellValueAsDouble(x2, y2);
		setOutputValue(outputLayer, orgAccCost, prevDstAccCost, dCost1, dCost2, dist, x2, y2, ccsID, iPoint);
	}


	private void setOutputValue(final IRasterLayer outputLayer,
			final double orgAccCost,
			final double prevDstAccCost,
			final double cost1,
			final double cost2,
			final double dist,
			final int x,
			final int y,
			final String ccsID,
			final int iPoint) {

		//orgAccCost += ((cost1 + cost2) * dist);
		final double dstAccCost = orgAccCost + ((cost1 + cost2) * dist);
		System.out.println("cost1: " + cost1 + " cost2: " + cost2);
		System.out.println("prevDstAccCost: " + prevDstAccCost + "  orgAccCost: " + orgAccCost);
		System.out.println(outputLayer.getName() + "[" + x + ", " + y + "]: " + dstAccCost);
		//TODO Esto puede pasar????
		if (dstAccCost < 0) {
			System.out.println("--------------------- dstAccCost < 0.0");
			return;
		}


		if (outputLayer.isNoDataValue(prevDstAccCost) || (prevDstAccCost > dstAccCost)) {
			//System.out.println("prevAccCost: " + prevAccCost + " > " + currentAccCost + " currentAccCost");
			//         System.out.println("cost1: " + cost1 + " cost2: " + cost2 + " prev: " + prevAccCost);
			//         System.out.println(outputLayer.getName() + "[" + x + ", " + y + "]: " + currentAccCost);
			System.out.println("SET!!!!!!!!!!");
			outputLayer.setCellValue(x, y, dstAccCost);
			m_ClosestPoint.setCellValue(x, y, iPoint);
			final Object[] cssID_cellValue = new Object[3];
			cssID_cellValue[0] = ccsID;
			cssID_cellValue[1] = new GridCell(x, y, iPoint);
			cssID_cellValue[2] = dstAccCost;

			final String x_y_id = String.valueOf(x) + "_" + String.valueOf(y) + "_" + String.valueOf(ccsID);
			if (!m_AdjPoints_Set.contains(x_y_id)) {
				//m_AdjPoints.add(cssID_GridCell_value);
				m_CentralPoints.add(cssID_cellValue);
				m_AdjPoints_Set.add(x_y_id);
			}
		}
	}


	//Based on SeparateShapesAlgorithm
	private IOutputChannel getMyOutputChannel(final String surfaceID) throws UnsupportedOutputChannelException {

		IOutputChannel channel;
		//TODO CHange FOLDER!!!!!!!!!
		final String sFilename = "/tmp/output_condacccost_" + surfaceID + ".tif";
		channel = new FileOutputChannel(sFilename);
		return channel;

	}


	private boolean canMoveTo(final MovementSurface surf1,
			final int valueSurf2) {

		if (surf1 == null) {
			return false;
		}
		return surf1.canMoveTo(valueSurf2);
	}


	private boolean canMove(final MovementSurface surf1,
			final MovementSurface surf2,
			final int valueSurf1,
			final int valueSurf2) {
		boolean canMove = false;
		if (m_Movement_Surfaces.isNoDataValue(valueSurf1) || m_Movement_Surfaces.isNoDataValue(valueSurf2)) {
			canMove = false;
		}
		else {
			if ((surf1 != null) && !surf1.canMoveTo(valueSurf2) && (surf2 != null)) {
				canMove = false;
			}
			else if ((surf1 != null) && surf1.canMoveTo(valueSurf2)) {
				// GREAT!! Go ahead!!
				canMove = true;
			}
			else {
				// If there is any of the other... then go ahead... :)
				canMove = true;
			}
		}

		if (valueSurf1 == valueSurf2) {
			//TODO
			// GREAT!! Go ahead!!
		}
		else {
			//TODO
			// GREAT!! Go ahead!!
		}
		return canMove;
	}


	private boolean isTrue(String v) {
		boolean isTrue = true;
		v = v.replace(" ", "");
		//TODO translate
		if ((v == null) || v.equalsIgnoreCase("FALSE") || v.equalsIgnoreCase("NO") || v.equalsIgnoreCase("0.0")
				|| v.equalsIgnoreCase("0") || v.equalsIgnoreCase("FALSO") || v.equalsIgnoreCase("NULL") 
				|| v.equalsIgnoreCase("NONE") || v.equalsIgnoreCase("")) {
			isTrue = false;
		}
		return isTrue;
	}

}
