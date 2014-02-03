package es.udc.sextante.gridAnalysis.CDC;

import java.util.Comparator;


public class XYGridcellValue_Comparator
         implements
            Comparator<Object[]> {

   /**
    * o1 and 02 must have the following structure:
    * 
    * <pre>
    * ccs_GridCell_value[0] = &quot;GLOBAL&quot;;
    * ccs_GridCell_value[1] = new GridCell(x, y, iPoint);
    * ccs_GridCell_value[2] = 0.0;
    * </pre>
    */
   public int compare(final Object[] o1,
                      final Object[] o2) {

      final Double value1 = (Double) o1[2];
      final Double value2 = (Double) o2[2];

      return value1.compareTo(value2);
   }
}
