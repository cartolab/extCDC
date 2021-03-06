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
package es.udc.sextante.gridAnalysis.CDC;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import es.unex.sextante.additionalInfo.AdditionalInfoMultipleInput;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.OutputFactory;
import es.unex.sextante.core.OutputObjectsSet;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.dataObjects.ITable;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.IteratorException;
import es.unex.sextante.exceptions.NullParameterValueException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
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
public class CDCAlgorithm
extends
GeoAlgorithm {

	private static final boolean DEBUG = false;

	public static final String            COST                           = "COST";
	public static final String            ORIG_DEST                      = "ORIG_DEST";
	public static final String            CONDITIONAL_COST_SURFACES      = "CONDITINAL_COST_SURFACES";
	public static final String            MOVEMENT_SURFACES              = "MOVEMENT_SURFACES";
	public static final String            MOVEMENT_SURFACES_GROUPS_TABLE = "MOVEMENT_SURFACES_GROUP";
	public static final String            MOVEMENT_CONSTRAINTS_TABLE     = "MOVEMENT_CONSTRAINTS";
	public static final String            DISTANCE                       = "DISTANCE";

	//public static final String THRESHOLD = "THRESHOLD";
	public static final String            OUTPUT                         = "OUTPUT_FOLDER";
	public static final String            OUTPUT_ACCCOST                 = "ACCCOST";
	public static final String            OUTPUT_ACCGLOBALCOST           = "ACCGLOBALCOST";
	public static final String            OUTPUT_CLOSESTPOINT            = "CLOSESTPOINT";
	public static final String            OUTPUT_CONDITIONAL_COSTS       = "COND_COST";

	private static final int              NO_DATA                        = -99999;

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
	private IRasterLayer                  output_GAccGlobalCost;
	private HashMap<String, IRasterLayer> output_CondAccCosts;

	private IRasterLayer                  m_ClosestPoint;
	private List<Object[]>                m_CentralPoints;
	//   private HashMap<String, ArrayList>    m_Cond_CentralPoints, m_Cond_AdjPoints;

	HashMap<Integer, MovementSurface>     surfacesMap;
	//HashMap<Integer, MovementSurface>     ccsurfacesMap;


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

            m_Parameters.addFilepath(OUTPUT, Sextante
                    .getText("Output_folder_path"), true, false, (String) null);

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

        final ITable movSurfGroupsTable = m_Parameters
                .getParameterValueAsTable(MOVEMENT_SURFACES_GROUPS_TABLE);

        //Init map of surfacesID and MovSurface
        initMovementSurfaceMap(movSurfGroupsTable);

		final ITable movConstraintTable = m_Parameters.getParameterValueAsTable(MOVEMENT_CONSTRAINTS_TABLE);

        //Init movement surfaces constraints
        initMovementSurfaceConstraints(movConstraintTable);

    }

    public OutputObjectsSet compute(ITable movConstraintTable,
            ITable movSurfGroupsTable,
            Collection<? extends IRasterLayer> costs_surfaces,
            IRasterLayer m_Movement_Surfaces, IRasterLayer m_Cost,
            IRasterLayer m_Orig_Dest, int m_iDistance, String outputPath,
            OutputFactory output_factory, AnalysisExtent analysis_extent)
            throws GeoAlgorithmExecutionException {

        this.m_Cost = m_Cost;
        this.m_Orig_Dest = m_Orig_Dest;
        this.m_Movement_Surfaces = m_Movement_Surfaces;
        this.m_iDistance = m_iDistance;

        m_AnalysisExtent = analysis_extent;
        m_OutputFactory = output_factory;

        //TODO Now It suppose that are ordered
        input_Cond_Costs = new HashMap<String, IRasterLayer>();
        output_CondAccCosts = new HashMap<String, IRasterLayer>();

        Iterator<? extends IRasterLayer> iter = costs_surfaces.iterator();
        while (iter.hasNext()) {
            final IRasterLayer cost_grid = iter.next();
            String costgridname = cost_grid.getName();
            input_Cond_Costs.put(costgridname, cost_grid);
        }

        m_Movement_Surfaces.setFullExtent();

        m_Movement_Surfaces.open();

        final Object input_crs = m_Movement_Surfaces.getCRS();

        try {
            m_Movement_Surfaces.postProcess();
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        m_iNX = m_Movement_Surfaces.getNX();
        m_iNY = m_Movement_Surfaces.getNY();

        //Init map of surfacesID and MovSurface
        initMovementSurfaceMap(movSurfGroupsTable);

        //Init movement surfaces constraints
        initMovementSurfaceConstraints(movConstraintTable);

        //COUNT DIFFERENT VALUES
        final HashSet<Integer> surfacesID = new HashSet<Integer>();
        for (int y = 0; y < m_iNY; y++) {
            for (int x = 0; x < m_iNX; x++) {
                final int value = m_Movement_Surfaces.getCellValueAsInt(x, y);
                if (!m_Movement_Surfaces.isNoDataValue(value)
                        && !surfacesID.contains(value)) {
                    surfacesID.add(value);
                }
            }
        }

        int x, y;
        int iPoint = 1;
        double dValue;

        m_CentralPoints = new ArrayList<Object[]>();

        String output = "acc_cost_output";
        File file = new File(outputPath + File.separator + output + ".tif");
        IOutputChannel channel = new FileOutputChannel(file.getAbsolutePath());
        output_GAccCost = m_OutputFactory.getNewRasterLayer(OUTPUT_ACCCOST,
                IRasterLayer.RASTER_DATA_TYPE_DOUBLE, m_AnalysisExtent, 1,
                channel, input_crs);

        output_GAccCost.setFullExtent();

        output_GAccCost.setNoDataValue(NO_DATA);
        output_GAccCost.assignNoData();

        //TODO Esto debe hacerse al final de todo al tener la capa ya creadita
        addOutputRasterLayer(OUTPUT_ACCCOST, OUTPUT_ACCCOST, 1, channel,
                output_GAccCost);

        output = "acc_cost_global_output";
        file = new File(outputPath + File.separator + output + ".tif");
        channel = new FileOutputChannel(file.getAbsolutePath());
        output_GAccGlobalCost = m_OutputFactory.getNewRasterLayer(OUTPUT_ACCGLOBALCOST,
                IRasterLayer.RASTER_DATA_TYPE_DOUBLE, m_AnalysisExtent, 1,
                channel, input_crs);

        output_GAccGlobalCost.setFullExtent();

        output_GAccGlobalCost.setNoDataValue(NO_DATA);
        output_GAccGlobalCost.assignNoData();

        //TODO Esto debe hacerse al final de todo al tener la capa ya creadita
        addOutputRasterLayer(OUTPUT_ACCGLOBALCOST, OUTPUT_ACCGLOBALCOST, 1, channel,
        		output_GAccGlobalCost);

        output = "closest_point_output";
        file = new File(outputPath + File.separator + output + ".tif");
        channel = new FileOutputChannel(file.getAbsolutePath());
        m_ClosestPoint = m_OutputFactory.getNewRasterLayer(OUTPUT_CLOSESTPOINT,
                IRasterLayer.RASTER_DATA_TYPE_INT, m_AnalysisExtent, 1,
                channel, input_crs);

        m_ClosestPoint.setFullExtent();

        m_ClosestPoint.setNoDataValue(NO_DATA);
        m_ClosestPoint.assignNoData();

        //TODO Esto debe hacerse al final de todo al tener la capa ya creadita
        addOutputRasterLayer(OUTPUT_CLOSESTPOINT, OUTPUT_CLOSESTPOINT, 1,
                channel, m_ClosestPoint);

        m_Cost.setWindowExtent(m_AnalysisExtent);
        m_Cost.setInterpolationMethod(IRasterLayer.INTERPOLATION_BSpline);

        m_Orig_Dest.setWindowExtent(m_AnalysisExtent);
        m_Orig_Dest
                .setInterpolationMethod(IRasterLayer.INTERPOLATION_NearestNeighbour);

        m_iNX = m_Cost.getNX();
        m_iNY = m_Cost.getNY();

        //CREATING OUTPUT RASTERS
        for (String k : input_Cond_Costs.keySet()) {
            final String name = "CONDCOST_" + k.toUpperCase();

            output = k + "_output";
            file = new File(outputPath + File.separator + output + ".tif");
            channel = new FileOutputChannel(file.getAbsolutePath());
            /////// REVISAR COMO SE USA AHORA LA EXTENT
            final IRasterLayer cac = m_OutputFactory.getNewRasterLayer(name,
                    IRasterLayer.RASTER_DATA_TYPE_DOUBLE, m_AnalysisExtent, 1,
                    channel, input_crs);

            cac.setWindowExtent(m_AnalysisExtent);

            cac.setNoDataValue(NO_DATA);
            cac.assignNoData();

            //TODO Esto debe hacerse al final de todo al tener la capa ya creadita
            addOutputRasterLayer(name, name, 1, channel, cac);

            cac.close();
            output_CondAccCosts.put(k, cac);
        }

        //INITIALIZING...
        for (y = 0; y < m_iNY; y++) {
            for (x = 0; x < m_iNX; x++) {
                dValue = m_Orig_Dest.getCellValueAsDouble(x, y);
                if (!m_Orig_Dest.isNoDataValue(dValue)) {
                    // Store the GridCell and of its CostSurface (-1 means Global Cost Surface)
                    final Object[] ccs_cellValue = new Object[3];
                    // "GLOBAL" if is simple... otherwise it is ccsID number
                    ccs_cellValue[0] = "GLOBAL";
                    ccs_cellValue[1] = new GridCell(x, y, iPoint);
                    ccs_cellValue[2] = 0.0;
                    m_CentralPoints.add(ccs_cellValue);
                    output_GAccCost.setCellValue(x, y, 0.0);
                    output_GAccGlobalCost.setCellValue(x, y, 0.0);
                    m_ClosestPoint.setCellValue(x, y, iPoint);
                    for (final String k : output_CondAccCosts.keySet()) {
                        final IRasterLayer out_acccost = output_CondAccCosts
                                .get(k);
                        //TODO Add on m_CentralPoints????

                        out_acccost.setCellValue(x, y, 0.0);

                    }

                    iPoint++;
                }
            }
        }

        if (m_iDistance != WINDOW5X5) {
        	if (DEBUG) {
        		calculateCost3X3Debug();
        	} else {
        		calculateCost3X3();
        	}
        }

        return m_OutputObjects;

    }


	@Override
    public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

	    final ITable movSurfGroupsTable = m_Parameters
                .getParameterValueAsTable(MOVEMENT_SURFACES_GROUPS_TABLE);

        final ITable movConstraintTable = m_Parameters
                .getParameterValueAsTable(MOVEMENT_CONSTRAINTS_TABLE);

        final ArrayList<IRasterLayer> costs_surfaces_array = m_Parameters
                .getParameterValueAsArrayList(CONDITIONAL_COST_SURFACES);

        m_Movement_Surfaces = m_Parameters
                .getParameterValueAsRasterLayer(MOVEMENT_SURFACES);
        m_Cost = m_Parameters.getParameterValueAsRasterLayer(COST);
        m_Orig_Dest = m_Parameters.getParameterValueAsRasterLayer(ORIG_DEST);
        m_iDistance = m_Parameters.getParameterValueAsInt(DISTANCE);
        String outputPath = m_Parameters.getParameterValueAsString(OUTPUT);

        compute(movConstraintTable, movSurfGroupsTable,
                costs_surfaces_array, m_Movement_Surfaces, m_Cost, m_Orig_Dest,
                m_iDistance, outputPath, m_OutputFactory, m_AnalysisExtent);

        return !m_Task.isCanceled();

	}


    private void initMovementSurfaceConstraints(ITable movConstraintTable)
            throws IteratorException {

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


        for (final IRecordsetIterator iter = movConstraintTable.iterator(); iter
                .hasNext();) {
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
            } /*else {
                //TODO
                System.out
                        .println("ERROR: Any surface with ID ---------------> "
                                + surfID);
            }*/
        }
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
            final String name = values[1].toString();
            final String css = values[2].toString();
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

		//System.out.println("--------------------------------   m_CentralPoints.size(): " + m_CentralPoints.size());

		int i, j;
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
		for (MovementSurface surface: surfacesMap.values()) {
			if (surface == null) {
				continue;
			}
			surface.openAll();
		}

		Collections.sort(m_CentralPoints, new XYGridcellValue_Comparator());
		//long useless = 0, total = 0;
        while (!m_CentralPoints.isEmpty()) {
        	boolean hasToSort = false;
        	Object[] ccsID_cellValue = m_CentralPoints.remove(0);

            if ((m_Task != null) && (m_Task.isCanceled())) {
                break;
            }

			cell = (GridCell) ccsID_cellValue[1];
			x1 = cell.getX();
			y1 = cell.getY();

			orgCostValue = m_Cost.getCellValueAsDouble(x1, y1);

			if ((orgCostValue <= 0) || m_Cost.isNoDataValue(orgCostValue)) {
				continue;
			}
			String orgSurfID_Name = (String) ccsID_cellValue[0];
			iPoint = (int) cell.getValue();
			orgSurfaceID = m_Movement_Surfaces.getCellValueAsInt(x1, y1);
			final MovementSurface orgSurface = surfacesMap.get(orgSurfaceID);
			final boolean isNode = orgSurface.isNode();
			IRasterLayer surface;
			if (orgSurfID_Name.equalsIgnoreCase("GLOBAL")) {
				String orgSurfaceName = orgSurface.getCCSName();
				for (i = -1; i < 2; i++) {
					for (j = -1; j < 2; j++) {
						x2 = x1 + i;
						y2 = y1 + j;
						if ((x1 == x2) && (y1 == y2)) {
							continue;
						}
						//boolean useful = false;
						dstCostValue = m_Cost.getCellValueAsDouble(x2, y2);

						//ALGORITHM CALCULUS
						// TODO �No llega con chequear > 0 ?
						if ((dstCostValue > 0) && !m_Cost.isNoDataValue(dstCostValue)) {
							dstSurfaceID = m_Movement_Surfaces.getCellValueAsInt(x2, y2);
							dist = dDist[i + 1][j + 1];

							if (canMoveTo(orgSurface, dstSurfaceID)){
								orgAccCost = output_GAccCost.getCellValueAsDouble(x1, y1);
								hasToSort |= setOutputValue(output_GAccCost, orgAccCost, orgCostValue, dstCostValue, dist, x2, y2, "GLOBAL", iPoint);
							}

							//TODO IMPORTANTE 
							// Esto hace que se entre varias veces en el CASd
							surface = input_Cond_Costs.get(orgSurfaceName);					
							if ((surface != null) && isNode && surface.getCellValueAsDouble(x2, y2)>-1) {
								//TODO 
								String new_ccsID = surface.getName();
								IRasterLayer outputRaster = output_CondAccCosts.get(new_ccsID);
								double cost2 = surface.getCellValueAsDouble(x2, y2);
								orgAccCost = output_GAccCost.getCellValueAsDouble(x1, y1);
								hasToSort |= setOutputValue(outputRaster, orgAccCost, orgCostValue, cost2, dist, x2, y2, new_ccsID, iPoint);
							}

							/*if (!useful) {
								useless++;
							}
							total++;*/
						}
					}
				}
			} else {
				surface = input_Cond_Costs.get(orgSurfID_Name);
				if (surface != null) {
					for (i = -1; i < 2; i++) {
						for (j = -1; j < 2; j++) {
							x2 = x1 + i;
							y2 = y1 + j;
							if ((x1 == x2) && (y1 == y2)) {
								continue;
							}
							//boolean useful = false;

							dstCostValue = m_Cost.getCellValueAsDouble(x2, y2);

							//ALGORITHM CALCULUS
							// TODO �No llega con chequear > 0 ?
							if ((dstCostValue > 0) && !m_Cost.isNoDataValue(dstCostValue)) {

								dstSurfaceID = m_Movement_Surfaces.getCellValueAsInt(x2, y2);
								final MovementSurface dstSurface = surfacesMap.get(dstSurfaceID);
								if (dstSurface == null) {
									continue;
								}
								dist = dDist[i + 1][j + 1];

								final IRasterLayer cac_output = output_CondAccCosts.get(orgSurfID_Name);
								final double cost2 = surface.getCellValueAsDouble(x2, y2);
								if (cost2 > -1) {
									final double cost1 = surface.getCellValueAsDouble(x1, y1);
									orgAccCost = cac_output.getCellValueAsDouble(x1, y1);
									hasToSort |= setOutputValue(cac_output, orgAccCost, cost1, cost2, dist, x2, y2, orgSurfID_Name, iPoint);
								}

								if (dstSurface.isNode() && dstSurface.getCCSName() == orgSurfID_Name){
									final double cost1 = surface.getCellValueAsDouble(x1, y1);
									orgAccCost = cac_output.getCellValueAsDouble(x1, y1);
									hasToSort |= setOutputValue(output_GAccCost, orgAccCost, cost1, dstCostValue, dist, x2, y2, "GLOBAL", iPoint);
								}

								/*if (!useful) {
									useless++;
								}
								total++;*/
							}
						}
					}
				}
			}
			if (hasToSort) {
				Collections.sort(m_CentralPoints, new XYGridcellValue_Comparator());
			}

		} // For para recorrer celdas

        //System.out.println("Iteraciones internas in�tiles: " + useless + " de " + total);
        
	}


	private void calculateCost3X3Debug() {

		System.out.println("--------------------------------   m_CentralPoints.size(): " + m_CentralPoints.size());

		int i, j;
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
			System.out.println("Surface[" + k + "]: " + surface);
			if (surface == null) {
				continue;
			}
			surface.openAll();
		}

		int count = 0;
		while (m_CentralPoints.size() != 0) {

            if ((m_Task != null) && (m_Task.isCanceled())) {
                break;
            }

			System.out.println("iter[[" + count++ + "]] --------------------------------   m_CentralPoints.size(): " + m_CentralPoints.size());
			System.out.println();
			System.out.println();
			System.out.println("------------ m_CentralPoints.size(): " + m_CentralPoints.size());

			//NachoV: Ahora acabo de poner esta ordenacion antes del iter... ¿¿¿Es correcto???
			Collections.sort(m_CentralPoints, new XYGridcellValue_Comparator());
			/*final Iterator iter = m_CentralPoints.iterator();
			while (iter.hasNext()) {
				final Object[] ccsID_cellValue = (Object[]) iter.next();
				final String ccsID = (String) ccsID_cellValue[0];
				cell = (GridCell) ccsID_cellValue[1];
				final Double value = (Double) ccsID_cellValue[2];
				x1 = cell.getX();
				y1 = cell.getY();
				System.out.println("x,y: (" + x1 + ", " + y1 + ") ccsID: [" + ccsID + "] Value: " + value);
			}
			System.out.println();
			System.out.println();*/

			final Object[] ccsID_cellValue = (Object[]) m_CentralPoints.get(0);
			m_CentralPoints.remove(0);
			String orgSurfID_Name = (String) ccsID_cellValue[0];
			cell = (GridCell) ccsID_cellValue[1];
			x1 = cell.getX();
			y1 = cell.getY();
			
			final Double value = (Double) ccsID_cellValue[2];
			System.out.println("\n********************");
			System.out.println("New Origen----> x,y: ("
					+ x1 + ", " + y1 + ") "
					+ orgSurfID_Name + " -- "
					+ value);
			System.out.println("********************");
			
			iPoint = (int) cell.getValue();
			orgCostValue = m_Cost.getCellValueAsDouble(x1, y1);
			orgSurfaceID = -1;
			orgSurfaceID = m_Movement_Surfaces.getCellValueAsInt(x1, y1);
			
			//Aviso de que ya se ha ido por esta rama y no es necesario comprobarlo otra vez
			
			for (i = -1; i < 2; i++) {
				for (j = -1; j < 2; j++) {
					x2 = x1 + i;
					y2 = y1 + j;
					if ((x1 == x2) && (y1 == y2)) {
						continue;
					}

					System.out.println("** New Dst cell: [" +x2 +", " + y2 +"] **");
					
					dist = dDist[i + 1][j + 1];
					dstCostValue = m_Cost.getCellValueAsDouble(x2, y2);
					dstSurfaceID = -1;
					dstSurfaceID = m_Movement_Surfaces.getCellValueAsInt(x2, y2);

					boolean CAS_done = false;
					
					//TODO --------------------------------------------- Ahora se debe mirar los costes condicionales, no???
					final MovementSurface orgSurface = surfacesMap.get(orgSurfaceID);
					final MovementSurface dstSurface = surfacesMap.get(dstSurfaceID);

					IRasterLayer surface = null;

					//ALGORITHM CALCULUS
					if (!m_Cost.isNoDataValue(orgCostValue) && !m_Cost.isNoDataValue(dstCostValue) && (orgCostValue > 0)
							&& (dstCostValue > 0)) {
						
						boolean canMove = false;
						canMove = canMoveTo(orgSurface, dstSurface.getID());
						
						if (orgSurfID_Name.equalsIgnoreCase("GLOBAL")) {
							//It means that the gridCell on m_CentralPoint belongs to Global Cost Surface
							for (String k: input_Cond_Costs.keySet()) {
	
								boolean dstHasSomeCondCostValue = false;

								//TODO IMPORTANTE 
								// Esto hace que se entre varias veces en el CASd
								surface = input_Cond_Costs.get(k);					
								if ((surface == null)) {
									continue;
								}

								surface.setWindowExtent(m_AnalysisExtent);
								surface.setInterpolationMethod(IRasterLayer.INTERPOLATION_BSpline);

								if (surface.getCellValueAsDouble(x2, y2)>-1) {
									dstHasSomeCondCostValue = true;
								}
								System.out.println("SCCk: " + k);
								if (dstHasSomeCondCostValue) {
									//TODO 
									if (orgSurface.isNode() && orgSurface.getCCSName()==k){
										String new_ccsID = surface.getName();
										IRasterLayer outputRaster = output_CondAccCosts.get(new_ccsID);
										double cost1 = orgCostValue;
										double cost2 = surface.getCellValueAsDouble(x2, y2);
										orgAccCost = output_GAccCost.getCellValueAsDouble(x1, y1);
										dPrevAccCost = outputRaster.getCellValueAsDouble(x2, y2);
										setOutputValueDebug(outputRaster, orgAccCost, dPrevAccCost, cost1, cost2, dist, x2, y2, new_ccsID, iPoint);								
									}
								}
								if (canMove && !CAS_done){
									System.out.println("---------------> CASd = CASo + d * ( CSo + CSd)");
									String new_ccsID = "GLOBAL"; //surface.getCCSName();
									IRasterLayer outputRaster = output_GAccCost;
									double cost1 = orgCostValue;
									double cost2 = dstCostValue;
									orgAccCost = output_GAccCost.getCellValueAsDouble(x1, y1);
									dPrevAccCost = outputRaster.getCellValueAsDouble(x2, y2);
									setOutputValueDebug(outputRaster, orgAccCost, dPrevAccCost, cost1, cost2, dist, x2, y2, new_ccsID, iPoint);
									CAS_done = true;
								}
							} // FOR of "D tiene valor en algún SCCi (1-n)"

						} else {
							//NO ES GLOBAL
							System.out.println("NO ES GLOBAL: [" + orgSurfID_Name + "]");
							IRasterLayer auxSurf = null;
							
							String surfName = "";
							for (String surf_name: input_Cond_Costs.keySet()) {
								//TODO NachoV rename ccsID a otro nombre mejor?
								if (surf_name == orgSurfID_Name) {
									auxSurf = input_Cond_Costs.get(surf_name);
									surfName = surf_name;
									break;
								}
							}
							if (auxSurf == null) {
								continue;
							}

							final boolean dstHasSCC = (auxSurf.getCellValueAsDouble(x2, y2)>-1);
							
							final IRasterLayer cac_output = output_CondAccCosts.get(surfName);
							if (dstHasSCC) {
								System.out.println("===============> CACid = CACio + d * ( CCio + CCid)");
								String new_ccsID = surfName;
								final IRasterLayer outputRaster = cac_output;
								final double cost1 = auxSurf.getCellValueAsDouble(x1, y1);
								final double cost2 = auxSurf.getCellValueAsDouble(x2, y2);
								//orgAccCost = value;//outputRaster.getCellValueAsDouble(x1, y1);
								orgAccCost = outputRaster.getCellValueAsDouble(x1, y1);
								dPrevAccCost = outputRaster.getCellValueAsDouble(x2, y2);
								setOutputValueDebug(outputRaster, orgAccCost, dPrevAccCost, cost1, cost2, dist, x2, y2, new_ccsID, iPoint);
							}

							if (dstSurface.isNode() && dstSurface.getCCSName()==surfName){
								System.out.println("NO_GLOBAL---------------> CASd = CACio + d * ( CCio + CSd)");
								String new_ccsID = "GLOBAL";
								final IRasterLayer outputRaster = output_GAccCost;								
								final double cost1 = auxSurf.getCellValueAsDouble(x1, y1);
								final double cost2 = dstCostValue;
								orgAccCost = cac_output.getCellValueAsDouble(x1, y1);
								dPrevAccCost = outputRaster.getCellValueAsDouble(x2, y2);
								setOutputValueDebug(outputRaster, orgAccCost, dPrevAccCost, cost1, cost2, dist, x2, y2, new_ccsID, iPoint);
							}
						}
					}
				}
			}
			// setProgressText(Integer.toString(m_CentralPoints.size()));

		} // For para recorrer celdas

	}


	private boolean setOutputValue(final IRasterLayer outputLayer,
			final double orgAccCost,
			final double cost1,
			final double cost2,
			final double dist,
			final int x,
			final int y,
			final String ccsID,
			final int iPoint) {

		final double dstAccCost = orgAccCost + ((cost1 + cost2) * dist);

		//TODO Esto puede pasar????
		if (dstAccCost < 0) {
			return false;
		}

		double prevDstAccCost = outputLayer.getCellValueAsDouble(x, y);
		if (outputLayer.isNoDataValue(prevDstAccCost) || (prevDstAccCost > dstAccCost)) {
			outputLayer.setCellValue(x, y, dstAccCost);
			if (output_GAccGlobalCost.isNoDataValue(output_GAccGlobalCost.getCellValueAsDouble(x, y))
					|| output_GAccGlobalCost.getCellValueAsDouble(x, y) > dstAccCost) {
				output_GAccGlobalCost.setCellValue(x, y, dstAccCost);
			}
			m_ClosestPoint.setCellValue(x, y, iPoint);
			final Object[] cssID_cellValue = new Object[3];
			cssID_cellValue[0] = ccsID;
			cssID_cellValue[1] = new GridCell(x, y, iPoint);
			cssID_cellValue[2] = dstAccCost;

			return m_CentralPoints.add(cssID_cellValue);
		}
		return false;
	}


	private void setOutputValueDebug(final IRasterLayer outputLayer,
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

		if (orgAccCost == -1) {
			System.out.print("");
		}

		System.out.println("cost1: " + cost1 + " cost2: "
				+ cost2);
		System.out.println("prevDstAccCost: " + prevDstAccCost
				+ "  orgAccCost: " + orgAccCost);
		System.out.println(outputLayer.getName() + "["
						+ x + ", " + y
						+ "] to set?: "
						+ dstAccCost);
		//TODO Esto puede pasar????
		if (dstAccCost < 0) {
			System.out.println(" Esto puede pasar???? --------------------- dstAccCost < 0.0");
			return;
		}

		if (outputLayer.isNoDataValue(prevDstAccCost) || (prevDstAccCost > dstAccCost)) {
			System.out.println("SET!!!!!!!!!! " + outputLayer.getOutputChannel());
			System.out.println("before: " + outputLayer.getCellValueAsDouble(x, y));
			outputLayer.setCellValue(x, y, dstAccCost);
			if (output_GAccGlobalCost.isNoDataValue(output_GAccGlobalCost.getCellValueAsDouble(x, y))
					|| output_GAccGlobalCost.getCellValueAsDouble(x, y) > dstAccCost) {
				System.out.println("also updating the global cost!");
				output_GAccGlobalCost.setCellValue(x, y, dstAccCost);
			}
			System.out.println("after: " + outputLayer.getCellValueAsDouble(x, y));
			if (outputLayer.getCellValueAsDouble(x, y) != dstAccCost) {
				System.out.println("Why!!!!!!!!!!!!??????? " + dstAccCost +" != "+ outputLayer.getCellValueAsDouble(x, y));
			}
			m_ClosestPoint.setCellValue(x, y, iPoint);
			final Object[] cssID_cellValue = new Object[3];
			cssID_cellValue[0] = ccsID;
			cssID_cellValue[1] = new GridCell(x, y, iPoint);
			cssID_cellValue[2] = dstAccCost;

			//final String x_y_id = String.valueOf(x) + "_" + String.valueOf(y) + "_" + String.valueOf(ccsID);
			//System.out.println("Size: " + m_AdjPointsMap.size());
			m_CentralPoints.add(cssID_cellValue);
		} else {
			System.out.println("Not set!! \n");
		}
	}


	private boolean canMoveTo(final MovementSurface surf1,
			final int valueSurf2) {

		if (surf1 == null) {
			return false;
		}
		return surf1.canMoveTo(valueSurf2);
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
