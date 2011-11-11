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
import es.unex.sextante.rasterWrappers.GridExtent;


/**
 * To compile this algorithm it must be place on gridAnalysis project of SEXTANTE
 * 
 * @author uve
 * 
 */
public class AlbertoConditionalCostAlgorithm
         extends
            GeoAlgorithm {

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
   private IRasterLayer                  m_AccCost;

   private IRasterLayer                  m_MovementSurfaces;
   private HashMap<String, IRasterLayer> m_Cost_grids;

   private IRasterLayer                  m_ClosestPoint;
   private ArrayList                     m_CentralPoints, m_AdjPoints;

   HashMap<Integer, MovementSurface>     surfacesMap;


   @Override
   public void defineCharacteristics() {

      final String[] sOptions = { Sextante.getText("Euclidean"), Sextante.getText("Chessboard"), Sextante.getText("Manhattan"),
               Sextante.getText("Chamfer 3:4"), " 5 X 5" };
      setName(Sextante.getText("Alberto_Conditional_Cost"));
      setGroup(Sextante.getText("ALBERTO"));
      setGeneratesUserDefinedRasterOutput(true);
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


   private void initConstraints() throws WrongParameterTypeException, WrongParameterIDException, NullParameterValueException,
                                 IteratorException {

      final ITable movConstraintTable = m_Parameters.getParameterValueAsTable(MOVEMENT_CONSTRAINTS_TABLE);
      final int numConstraint = movConstraintTable.getFieldCount();

      final ArrayList<Integer> ids = new ArrayList<Integer>();
      for (int i = 1; i < numConstraint; i++) {
         final int id = Integer.parseInt(movConstraintTable.getFieldName(i));
         ids.add(id);
      }

      //TODO Default must be more elegant!!!!
      final HashMap<Integer, Boolean> defaultConstraintsMap = new HashMap<Integer, Boolean>();
      for (final Iterator<Integer> iter = ids.iterator(); iter.hasNext();) {
         final int id = iter.next();
         defaultConstraintsMap.put(id, true);
      }


      for (final IRecordsetIterator iter = movConstraintTable.iterator(); iter.hasNext();) {
         final IRecord values = iter.next();

         final HashMap<Integer, Boolean> constraintsMap = new HashMap<Integer, Boolean>();
         //TODO It must be a hashMap with surfacesID, not a Array
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

      // Mover
      // /var/tmp/sextante_svn/sextante/trunk/soft/sextante_lib/dist/sextante_gridAnalysis-0.55.jar
      // a
      // /var/tmp/jbsig_1.9/_fwAndami/gvSIG/extensiones/es.unex.sextante/.
      // cp /var/tmp/sextante_svn/sextante/trunk/soft/sextante_lib/dist/sextante_gridAnalysis-0.55.jar /var/tmp/jbsig_1.9/_fwAndami/gvSIG/extensiones/es.unex.sextante/.

      final ITable movSurfGroupsTable = m_Parameters.getParameterValueAsTable(MOVEMENT_SURFACES_GROUPS_TABLE);

      //TODO Create objects!!
      final ArrayList<IRasterLayer> costs_surfaces_array = m_Parameters.getParameterValueAsArrayList(CONDITIONAL_COST_SURFACES);

      //TODO Now It suppose that are ordered
      m_Cost_grids = new HashMap<String, IRasterLayer>();
      for (int i = 0; i < costs_surfaces_array.size(); i++) {
         final IRasterLayer cost_grid = costs_surfaces_array.get(i);
         m_Cost_grids.put(cost_grid.getName(), cost_grid);
      }

      m_MovementSurfaces = m_Parameters.getParameterValueAsRasterLayer(MOVEMENT_SURFACES);
      m_MovementSurfaces.setFullExtent();

      m_MovementSurfaces.open();

      final Object input_crs = m_MovementSurfaces.getCRS();
      final GridExtent input_extent = new GridExtent(m_MovementSurfaces);

      try {
         m_MovementSurfaces.postProcess();
      }
      catch (final Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      m_iNX = m_MovementSurfaces.getNX();
      m_iNY = m_MovementSurfaces.getNY();

      //COUNT DIFFERENT VALUES
      final HashSet<Integer> surfacesID = new HashSet<Integer>();
      final int counter = 0;
      for (int y = 0; y < m_iNY; y++) {
         for (int x = 0; x < m_iNX; x++) {
            final int value = m_MovementSurfaces.getCellValueAsInt(x, y);
            //				if (value == ORG_DST_DATA){
            //					continue;
            //				}
            if (!m_MovementSurfaces.isNoDataValue(value) && !surfacesID.contains(value)) {
               surfacesID.add(value);
            }
         }
      }

      //Init map of surfacesID and MovSurface
      surfacesMap = new HashMap<Integer, MovementSurface>();
      for (final IRecordsetIterator iter = movSurfGroupsTable.iterator(); iter.hasNext();) {
         final IRecord record = iter.next();
         //0 "Z_Class";
         //1" Z_Name";
         //2" MZ_Group";
         //3" Cond_Cost";
         //4" Z_CondCostGrid";
         //5" CZ_Link";
         //6" Assoc_with"
         final Object[] values = record.getValues();
         final int id = Integer.parseInt(values[0].toString());
         final String name = (String) values[1];
         final String group = (String) values[2];
         final boolean conditional_cost = isTrue(values[3].toString());
         //TODO Number or Name of the GRID???
         final String cost_grid = values[4].toString().replaceAll("\"", "");
         System.out.println("COST_GRID: " + cost_grid);
         final IRasterLayer conditional_cost_grid = m_Cost_grids.get(cost_grid);
         final boolean link = isTrue(values[5].toString());
         int associated_with;
         try {
            associated_with = Integer.parseInt(values[6].toString());
         }
         catch (final Exception e) {
            associated_with = -1;
         }
         final MovementSurface ms = new MovementSurface(id, name, group, conditional_cost, conditional_cost_grid, link,
                  associated_with);
         surfacesMap.put(id, ms);
      }

      //Init map of surfacesID
      initConstraints();

      //TODO HOW TO SAVE IN A USER FOLDER!???!?!??!?!
      // TEST /////////////////////
      final IRasterLayer[] result = new IRasterLayer[surfacesID.size()];
      final Iterator<Integer> iter = surfacesID.iterator();
      for (int i = 0; iter.hasNext(); i++) {
         final int id = iter.next();
         final String name = OUTPUT_CONDITIONAL_COSTS + Integer.toString(i);
         final IOutputChannel channel = getMyOutputChannel(String.valueOf(id));

         result[i] = m_OutputFactory.getNewRasterLayer(name, IRasterLayer.RASTER_DATA_TYPE_DOUBLE, input_extent, 1, channel,
                  input_crs);

         result[i].setWindowExtent(input_extent);
         result[i].open();
         //result[i].assignNoData();
         for (int x = 0; x < result[i].getNX(); x++) {
            for (int y = 0; y < result[i].getNY(); y++) {
               result[i].setCellValue(x, y, id);
            }
         }

         //TODO Esto debe hacerse al final de todo al tener la capa ya creadita
         addOutputRasterLayer(name, name, 1, channel, result[i]);

         result[i].close();
      }
      /////////////////////////////////////////////////////////////////

      int x, y;
      int iPoint = 1;
      double dValue;

      m_CentralPoints = new ArrayList();

      m_Cost = m_Parameters.getParameterValueAsRasterLayer(COST);
      m_Orig_Dest = m_Parameters.getParameterValueAsRasterLayer(ORIG_DEST);
      m_iDistance = m_Parameters.getParameterValueAsInt(DISTANCE);

      //m_dThreshold = m_Parameters.getParameterValueAsDouble(THRESHOLD);

      m_AccCost = getNewRasterLayer(OUTPUT_ACCCOST, Sextante.getText("Accumulated_cost"), IRasterLayer.RASTER_DATA_TYPE_DOUBLE);
      m_ClosestPoint = getNewRasterLayer(OUTPUT_CLOSESTPOINT, Sextante.getText("Closest_points"),
               IRasterLayer.RASTER_DATA_TYPE_INT);

      final GridExtent extent = m_AccCost.getWindowGridExtent();

      m_Cost.setWindowExtent(extent);
      m_Cost.setInterpolationMethod(IRasterLayer.INTERPOLATION_BSpline);

      m_Orig_Dest.setWindowExtent(extent);
      m_Orig_Dest.setInterpolationMethod(IRasterLayer.INTERPOLATION_NearestNeighbour);

      m_iNX = m_Cost.getNX();
      m_iNY = m_Cost.getNY();

      m_AccCost.setNoDataValue(NO_DATA);
      m_AccCost.assignNoData();

      m_ClosestPoint.setNoDataValue(NO_DATA);
      m_ClosestPoint.assignNoData();

      //INITIALIZING...
      for (y = 0; y < m_iNY; y++) {
         for (x = 0; x < m_iNX; x++) {
            dValue = m_Orig_Dest.getCellValueAsDouble(x, y);
            if ((dValue != 0.0) && !m_Orig_Dest.isNoDataValue(dValue)) {
               m_CentralPoints.add(new GridCell(x, y, iPoint));
               m_AccCost.setCellValue(x, y, 0.0);
               m_ClosestPoint.setCellValue(x, y, iPoint);
               iPoint++;
            }
         }
      }

      if (m_iDistance != WINDOW5X5) {
         calculateCost3X3();
      }
      //				else{
      //					calculateCost5X5();
      //				}

      return !m_Task.isCanceled();

   }


   //	private void calculateCost5X5() {
   //
   //		int i, j;
   //		int iPt;
   //		int iPoint;
   //		int x,y,x2,y2;
   //		double dAccCost;
   //		double dCost;
   //		double dPrevAccCost;
   //		GridCell cell;
   //
   //		double dDist[][] = new double [5][5];
   //
   //		for (i = -2; i < 3; i++) {
   //			for (j = -1; j < 2; j++) {
   //				dDist[i+2][j+2] = Math.sqrt(i*i+j*j);
   //			}
   //		}
   //
   //		m_AdjPoints = new ArrayList();
   //		while (m_CentralPoints.size()!=0 && !m_Task.isCanceled()){
   //			for (iPt=0; iPt<m_CentralPoints.size();iPt++){
   //				cell = (GridCell) m_CentralPoints.get(iPt);
   //				x = cell.getX();
   //				y = cell.getY();
   //				iPoint = (int) cell.getValue();
   //				for (i = -2; i < 3; i++) {
   //					for (j = -2; j < 3; j++) {
   //						x2 = x + i;
   //						y2 = y + j;
   //						dCost = getCostTo(x,y, i, j);
   //						if (dCost != NO_DATA){
   //							dAccCost = m_AccCost.getCellValueAsDouble(x, y);
   //							dAccCost += (dCost * dDist[i+2][j+2]);
   //							dPrevAccCost = m_AccCost.getCellValueAsDouble(x2,y2);
   //							if (m_AccCost.isNoDataValue(dPrevAccCost) ||
   //									dPrevAccCost > dAccCost){
   //								m_AccCost.setCellValue(x2, y2, dAccCost);
   //								m_ClosestPoint.setCellValue(x2, y2, iPoint);
   //								m_AdjPoints.add(new GridCell(x2, y2, iPoint));
   //							}
   //						}
   //					}
   //				}
   //			}
   //
   //			m_CentralPoints = m_AdjPoints;
   //			m_AdjPoints = new ArrayList();
   //		}
   //
   //	}

   private double getCostTo(final int x,
                            final int y,
                            final int i,
                            final int j) {

      int n, nMax;
      int iCells = 0;
      double di, dj;
      double dCost = 0;
      double dPartialCost;

      if ((i == 0) && (j == 0)) {
         return 0;
      }

      if (i > j) {
         dj = Math.abs((double) j / (double) i) * Math.signum(j);
         di = Math.signum(i);
         nMax = Math.abs(i);
      }
      else {
         di = Math.abs((double) i / (double) j) * Math.signum(i);
         dj = Math.signum(j);
         nMax = Math.abs(j);
      }

      double ii = 0;
      double jj = 0;
      for (n = 0; n <= nMax; n++, ii += di, jj += dj) {
         dPartialCost = m_Cost.getCellValueAsDouble((int) (x + ii), (int) (y + jj));
         if (m_Cost.isNoDataValue(dPartialCost) || (dPartialCost <= 0)) {
            return NO_DATA;
         }
         else {
            dCost += dPartialCost;
            iCells++;
         }
      }

      return dCost / iCells;

   }


   private void calculateCost3X3() {

      int i, j;
      int iPt;
      int iPoint;
      int x, y, x2, y2;
      double dAccCost;
      double dCost1, dCost2;
      int dSurface1, dSurface2;
      double dPrevAccCost;
      GridCell cell;

      double dDist[][] = new double[3][3];

      switch (m_iDistance) {
         //TODO Dividir todo entre 2 para reducir computación luego
         case EUCLIDEAN:
         default:
            for (i = -1; i < 2; i++) {
               for (j = -1; j < 2; j++) {
                  dDist[i + 1][j + 1] = Math.sqrt(i * i + j * j);
               }
            }
            break;
         case CHESSBOARD:
            final double chessboard[][] = { { 1, 1, 1 }, { 1, 0, 1 }, { 1, 1, 1 } };
            dDist = chessboard;
            break;
         case MANHATTAN:
            final double manhattan[][] = { { 2, 1, 2 }, { 1, 0, 1 }, { 2, 1, 2 } };
            dDist = manhattan;
            break;
         case CHAMFER:
            final double chamfer[][] = { { 4, 3, 4 }, { 3, 0, 3 }, { 4, 3, 4 } };
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
      while ((m_CentralPoints.size() != 0) && !m_Task.isCanceled()) {
         System.out.println(count++ + "--------------------------------   m_CentralPoints.size(): " + m_CentralPoints.size());
         for (iPt = 0; iPt < m_CentralPoints.size(); iPt++) {
            cell = (GridCell) m_CentralPoints.get(iPt);
            x = cell.getX();
            y = cell.getY();
            iPoint = (int) cell.getValue();
            dCost1 = m_Cost.getCellValueAsDouble(x, y);
            dSurface1 = m_MovementSurfaces.getCellValueAsInt(x, y);
            for (i = -1; i < 2; i++) {
               for (j = -1; j < 2; j++) {
                  x2 = x + i;
                  y2 = y + j;
                  dCost2 = m_Cost.getCellValueAsDouble(x2, y2);
                  dSurface2 = m_MovementSurfaces.getCellValueAsInt(x2, y2);

                  //TODO --------------------------------------------- Ahora se debe mirar los costes condicionales, no???
                  final MovementSurface surf1 = surfacesMap.get(dSurface1);
                  final MovementSurface surf2 = surfacesMap.get(dSurface2);

                  //Check if there are cost cell with values on the conditinal grids
                  boolean hasValue = false;
                  for (int k = 0; k < surfacesMap.size(); k++) {
                     final MovementSurface surface = surfacesMap.get(k);

                     //System.out.println("Surface[" + k + "]: " + surface);
                     if (surface == null) {
                        continue;
                     }
                     hasValue = surface.hasCostValueAt(x2, y2);
                     System.out.println("surface[" + k + "].value(" + x2 + "," + y2 + ")=" + hasValue);
                  }


                  if (!m_Cost.isNoDataValue(dCost1) && !m_Cost.isNoDataValue(dCost2) && (dCost1 > 0) && (dCost2 > 0)
                      && canMove(surf1, surf2, dSurface1, dSurface2)) {

                     dAccCost = m_AccCost.getCellValueAsDouble(x, y);
                     //TODO [NACHO] Se puede quitar el /2 en la matriz dDist y se ahorra computación
                     dAccCost += ((dCost1 + dCost2) / 2.0 * dDist[i + 1][j + 1]);
                     dPrevAccCost = m_AccCost.getCellValueAsDouble(x2, y2);
                     if (m_AccCost.isNoDataValue(dPrevAccCost) || (dPrevAccCost > dAccCost)) {
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


   //Based on SeparateShapesAlgorithm
   private IOutputChannel getMyOutputChannel(final String surfaceID) throws UnsupportedOutputChannelException {

      IOutputChannel channel;
      //TODO CHange FOLDER!!!!!!!!!
      final String sFilename = "/tmp/output_condacccost_" + surfaceID + ".tif";
      channel = new FileOutputChannel(sFilename);
      return channel;

   }


   private boolean canMove(final MovementSurface surf1,
                           final MovementSurface surf2,
                           final int dSurface1,
                           final int dSurface2) {
      boolean canMove = false;
      if (m_MovementSurfaces.isNoDataValue(dSurface1) || m_MovementSurfaces.isNoDataValue(dSurface2)) {
         //System.out.println("Surface " + dSurface1 + " CAN NOT move to " + dSurface2);
         canMove = false;
      }
      else {
         if ((surf1 != null) && !surf1.canMoveTo(dSurface2) && (surf2 != null)) {
            //System.out.println("__Surface " + dSurface1 + " CAN NOT move to " + dSurface2);
            canMove = false;
         }
         else if ((surf1 != null) && surf1.canMoveTo(dSurface2)) {
            // GREAT!! Go ahead!!
            //System.out.println("SURFACE[" + dSurface1 + "] MOVE TO " + dSurface2);
            canMove = true;
         }
         else {
            // If there is any of the other... then go ahead... :)
            canMove = true;
         }
      }

      if (dSurface1 == dSurface2) {
         //TODO
         // GREAT!! Go ahead!!
      }
      else {
         //TODO
         // GREAT!! Go ahead!!
      }
      return canMove;
   }


   private boolean isTrue(final String v) {
      boolean isTrue = true;
      //TODO translate
      if ((v == null) || v.equalsIgnoreCase("FALSE") || v.equalsIgnoreCase("NO") || v.equalsIgnoreCase("0.0")
          || v.equalsIgnoreCase("0") || v.equalsIgnoreCase("FALSO") || v.equalsIgnoreCase("NULL") || v.equalsIgnoreCase("NONE")) {
         isTrue = false;
      }
      return isTrue;
   }

}
