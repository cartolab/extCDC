package es.udc.sextante.gridAnalysis.conditionalCost_alberto;

import java.util.HashMap;

import es.unex.sextante.dataObjects.IRasterLayer;

public class MovementSurface {

   private final int                 ID_CLASS;
   private final String              NAME;
   private final String              CSS;
   private final boolean             IS_NODE;
   private final IRasterLayer        CONDITIONAL_COST_GRID;

   private HashMap<Integer, Boolean> movConstraintsMap = new HashMap<Integer, Boolean>();


   public MovementSurface(final int id) {
      ID_CLASS = id;
      NAME = String.valueOf(id);
      CSS = "";
      IS_NODE = false;
      CONDITIONAL_COST_GRID = null;
   }

   public MovementSurface(final int id_class,
                          final String name,
                          final String css,
                          final boolean is_node,
                          final IRasterLayer conditional_cost_grid) {

      ID_CLASS = id_class;
      NAME = name;
      CSS = css;
      IS_NODE = is_node;
      CONDITIONAL_COST_GRID = conditional_cost_grid;
   }


   public void setMovConstraints(final HashMap<Integer, Boolean> constraintsMap) {
      movConstraintsMap = constraintsMap;
   }


   public boolean canMoveTo(final int surfaceID) {
      //TODO WARNING By default it returns True (if that surfaceID DOES NOT exists)
      //TODO
      //System.out.println("ID: "+ this.ID +" to surfaceID: " + surfaceID + " movConstraintsMap: "+ movConstraintsMap);

      final Boolean value = movConstraintsMap.get(surfaceID);
      if ((value == null) || value.booleanValue()) {
         return true;
      }
      else {
         return false;
      }
   }


   public boolean hasConditionalCostValueAt(final int x,
                                            final int y) {

      if (CONDITIONAL_COST_GRID != null) {
         final double value = CONDITIONAL_COST_GRID.getCellValueAsDouble(x, y);
         if (isValidConditionalCost(value)) {
            return true;
         }
      }
      return false;
   }


   public boolean isValidConditionalCost(final double value) {
      if ((value > 0) && !CONDITIONAL_COST_GRID.isNoDataValue(value)) {
         return true;
      }
      return false;
   }


   //   public void setIsAssocietedWithOther(final boolean b) {
   //      IS_ASSOCIATED_FROM_OTHER = b;
   //   }


   //   public boolean isAssociatedWithOther() {
   //      return IS_ASSOCIATED_FROM_OTHER;
   //   }


   public void openAll() {
      //TODO
      if (CONDITIONAL_COST_GRID != null) {
         CONDITIONAL_COST_GRID.setFullExtent();
      }
   }


   public int getID() {
      return ID_CLASS;
   }


   public String getGroup() {
      return CSS;
   }


   public boolean hasConditionalCost() {
      return IS_NODE;
   }

//
//   public boolean dependsOn(final int id) {
//      if (id == this.DEPENDS_ON_ZONE) {
//         return true;
//      }
//      return false;
//   }
//
//
//   public boolean isDependent() {
//      if (this.DEPENDS_ON_ZONE > -1) {
//         return true;
//      }
//      return false;
//   }


   public double getCCValueAt(final int x,
                              final int y) {
      if (CONDITIONAL_COST_GRID != null) {
         return CONDITIONAL_COST_GRID.getCellValueAsDouble(x, y);
      }
      else {
         System.out.println("ERROR: This Surface has no Conditional Surface!!!");
         return -1;
      }
   }


   public String getCCSName() {
      String name = "";
      if (CONDITIONAL_COST_GRID != null) {
         name = CONDITIONAL_COST_GRID.getName();
      }
      return name;
   }
   
   public boolean isNode(){
	   return IS_NODE;
   }

}
