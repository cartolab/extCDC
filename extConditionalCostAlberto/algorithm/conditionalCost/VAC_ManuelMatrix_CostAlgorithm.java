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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.rasterWrappers.GridWrapper;


/**
 * To compile this algorithm it must be place on gridAnalysis project of SEXTANTE
 * 
 * @author uve
 * 
 */
public class VAC_ManuelMatrix_CostAlgorithm
         extends
            GeoAlgorithm {

   public static final String LAYER          = "LAYER";
   public static final String LINKID_GRID    = "LINKID_GRID";
   public static final String LINKTIME_GRID  = "LINKTIME_GRID";
   public static final String LINKS_MATRIX   = "LINKS_MATRIX";
   //public static final String INTERPOLATE    = "INTERPOLATE";

   //public static final String THRESHOLD = "THRESHOLD";
   public static final String OUTPUT_ACCCOST = "ACCCOST";

   private static final int   NO_DATA        = -1;
   private static final int   ORG_DST_DATA   = 0;

   private IVectorLayer       m_Layer;
   private IRasterLayer       m_LinkIdGrid;
   private IRasterLayer       m_LinkTimeGrid;

   private IRasterLayer       m_OutputAccCost;


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("VAC_COST_MANUAL_matrix"));
      setGroup(Sextante.getText("ALBERTO"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("ORIGIN_layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POINT,
                  true);
         //, Sextante.getText("Raster_layers"), AdditionalInfoMultipleInput.DATA_TYPE_BAND, true);
         m_Parameters.addInputRasterLayer(LINKID_GRID, LINKID_GRID, true);
         m_Parameters.addInputRasterLayer(LINKTIME_GRID, LINKTIME_GRID, true);

         //m_Parameters.addBoolean(INTERPOLATE, Sextante.getText("Use_interpolation"), true);
         m_Parameters.addFilepath(LINKS_MATRIX, "Link Matrix", false, true, ".csv");
         //m_Parameters.addInputTable(LINKS_MATRIX, "LINKS_MATRIX", true);
         addOutputRasterLayer(OUTPUT_ACCCOST, Sextante.getText("VAC_Accumulated_cost"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
      final int i;
      final int j;
      int iTotalProgress;
      int iProgress = 0;
      final int iLayer;
      //      int iShapeCount;
      int orgIdLinkValue = -1;
      double orgTime2LinkValue = -1;
      boolean bInterpolate;
      final IRasterLayer grid;

      m_Layer = m_Parameters.getParameterValueAsVectorLayer(LAYER);

      final String matrixTable = m_Parameters.getParameterValueAsString(LINKS_MATRIX);
      m_LinkIdGrid = m_Parameters.getParameterValueAsRasterLayer(LINKID_GRID);
      m_LinkTimeGrid = m_Parameters.getParameterValueAsRasterLayer(LINKTIME_GRID);


      //      final GridExtent m_Extend = m_LinkTimeGrid.getWindowGridExtent();

      bInterpolate = false; //m_Parameters.getParameterValueAsBoolean(INTERPOLATE);

      m_OutputAccCost = getNewRasterLayer(OUTPUT_ACCCOST, m_Layer.getName() + "_VACAccCost", IRasterLayer.RASTER_DATA_TYPE_DOUBLE);
      //m_OutputAccCost.setNoDataValue(NO_DATA);
      //final GridExtent extent = m_OutputAccCost.getWindowGridExtent();
      m_OutputAccCost.setWindowExtent(m_AnalysisExtent);
      m_OutputAccCost.assignNoData();

      m_LinkTimeGrid.setWindowExtent(m_AnalysisExtent);
      m_LinkIdGrid.setWindowExtent(m_AnalysisExtent);

      //BASED ON ExtendsPointLayerWithGridAlgorithm
      //iShapeCount = m_Layer.getShapesCount();
      //dValues = new double[m_LinkIdGrid.size()][iShapeCount];
      //iTotalProgress = m_LinkIdGrid.getNX() * m_LinkIdGrid.getNY();
      final IFeatureIterator iter = m_Layer.iterator();

      m_LinkIdGrid.setFullExtent();
      m_LinkTimeGrid.setFullExtent();
      if (bInterpolate == false) {
         m_LinkIdGrid.setInterpolationMethod(GridWrapper.INTERPOLATION_NearestNeighbour);
         m_LinkTimeGrid.setInterpolationMethod(GridWrapper.INTERPOLATION_NearestNeighbour);
      }
      m_LinkIdGrid.open();
      m_LinkTimeGrid.open();

      //      System.out.println("MAX ID: " + m_LinkIdGrid.getMaxValue());
      //      System.out.println("Min ID: " + m_LinkIdGrid.getMinValue());

      //TODO Debería de tener un sólo punto... pero por si acaso
      //TODO HACER PARA QUE PUEDA TENER MUCHOS PUNTOS
      while (iter.hasNext()) {
         final IFeature feature = iter.next();
         final Geometry geom = feature.getGeometry();
         final Coordinate[] coords = geom.getCoordinates();
         orgTime2LinkValue = m_LinkTimeGrid.getValueAt(coords[0].x, coords[0].y);
         orgIdLinkValue = (int) m_LinkIdGrid.getValueAt(coords[0].x, coords[0].y);
         System.out.println(coords[0].x + "  -  " + coords[0].y + "  ID: " + orgIdLinkValue);
         break;
      }
      iter.close();

      final HashMap<Integer, HashMap> timeLinksMap = new HashMap<Integer, HashMap>();

      final File matrixFile = new File(matrixTable);
      FileInputStream fis = null;
      BufferedInputStream bis = null;
      DataInputStream dis = null;

      try {
         fis = new FileInputStream(matrixFile);

         // Here BufferedInputStream is added for fast reading.
         bis = new BufferedInputStream(fis);
         dis = new DataInputStream(bis);

         // dis.available() returns 0 if the file does not have more lines.
         String[] headerNames = null;
         while (dis.available() != 0) {

            //           final IRecordsetIterator tableIter = matrixTable.iterator();
            //           for (; tableIter.hasNext();) {

            final String line = dis.readLine();
            final String[] values = line.split(";");
            //final IRecord rec = new gvRecord();
            if (line.startsWith("ENLACE")) {
               headerNames = values;
               continue;
            }
            final String aux = values[0];
            final int link_id1 = Integer.parseInt(aux);

            final HashMap<String, Object> recMap = new HashMap<String, Object>();
            for (int k = 1; k < headerNames.length; k++) {
               final String fieldName = headerNames[k];
               System.out.println(fieldName + ": " + values[k]);
               recMap.put(fieldName, values[k]);
            }

            timeLinksMap.put(link_id1, recMap);
            //           }


            //        // this statement reads the line from the file and print it to
            //          // the console.
            //          System.out.println(dis.readLine());
         }

         // dispose all the resources after using them.
         fis.close();
         bis.close();
         dis.close();

      }
      catch (final FileNotFoundException e) {
         e.printStackTrace();
      }
      catch (final IOException e) {
         e.printStackTrace();
      }


      iTotalProgress = m_LinkIdGrid.getNX();

      final int table_link_value = -1;
      for (int x = 0; (x < m_LinkIdGrid.getNX()) && setProgress(iProgress, iTotalProgress); x++) {
         iProgress++;
         System.out.println(iProgress + " / " + iTotalProgress);
         for (int y = 0; y < m_LinkIdGrid.getNY(); y++) {
            //m_OutputAccCost.setNoData(x, y);

            final int dstIdLinkValue = (int) m_LinkIdGrid.getCellValueInLayerCoords(x, y, 0);
            if (m_LinkIdGrid.isNoDataValue(dstIdLinkValue)) {
               continue;
            }
            final double dstTime2LinkValue = m_LinkTimeGrid.getCellValueInLayerCoords(x, y, 0);
            if (dstTime2LinkValue < 0) {
               continue;
            }
            //System.out.println(x + ": " + dstIdLinkValue);
            double time_between_link = -1;
            //            if (dstIdLinkValue == 0) {
            //               System.out.println(x + ": " + dstIdLinkValue);
            //            }
            //            boolean found = false;
            //            if (table_link_value != new Double(dstTime2LinkValue).intValue()) {
            //               final IRecordsetIterator tableIter = matrixTable.iterator();
            //               for (; tableIter.hasNext();) {
            //                  final IRecord rec = tableIter.next();
            //                  final String aux = (String) rec.getValue(0);
            //                  final int link_id1 = Integer.parseInt(aux);
            //                  if (link_id1 == dstIdLinkValue) {
            //                     found = true;
            //                     //                     //OJO: WARNING: multiplico las celdas de VAC por 4.5 seg. (ASEGURAR QUE SUMAN 1)
            //                     //                     time_between_link = Double.parseDouble((rec.getValue((int) orgIdLinkValue)).toString()) * 4.5;
            //                     //TODO: Ahora se tiene el valor en segundos en las celdas
            //                     time_between_link = Double.parseDouble((rec.getValue((int) orgIdLinkValue)).toString());
            //                     break;
            //                  }
            //               }
            //            }
            //            if (!found) {
            //               System.out.println("No se ha encontrado valor para: " + dstIdLinkValue);
            //            }

            //SE LO ACABO DE QUITAR ESTE IF_ELSE
            //            if (table_link_value == new Double(dstTime2LinkValue).intValue()) {
            //               System.out.println("No se ha encontrado valor para: " + dstIdLinkValue);
            //            }
            //            else {
            final HashMap rec = timeLinksMap.get(dstIdLinkValue);
            if (rec != null) {
               ////OJO: WARNING: multiplico las celdas de VAC por 4.5 seg. (ASEGURAR QUE SUMAN 1)
               //             //                     time_between_link = Double.parseDouble((rec.getValue((int) orgIdLinkValue)).toString()) * 4.5;
               //             //TODO: Ahora se tiene el valor en segundos en las celdas
               time_between_link = Double.parseDouble(rec.get(String.valueOf(orgIdLinkValue)).toString());
            }
            //            }

            if (time_between_link != -1) {
               final double vac_cost = orgTime2LinkValue + dstTime2LinkValue + time_between_link;
               m_OutputAccCost.setCellValue(x, y, vac_cost);
            }
         }
      }

      // TODO Auto-generated method stub
      return !m_Task.isCanceled();
   }

}
