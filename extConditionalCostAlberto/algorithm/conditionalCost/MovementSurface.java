package es.udc.sextante.gridAnalysis.conditionalCost_alberto;

import java.util.HashMap;

import es.unex.sextante.dataObjects.IRasterLayer;

public class MovementSurface {

   private final int                 ID;
   private final String              NAME;
   private final String              GROUP;
   private final boolean             HAS_CONDITIONAL_COST;
   private final IRasterLayer        CONDITIONAL_COST_GRID;
   private final boolean             LINK;
   private final int                 DEPENDS_ON_ZONE;
   //   private boolean                   IS_ASSOCIATED_FROM_OTHER;

   private HashMap<Integer, Boolean> movConstraintsMap = new HashMap<Integer, Boolean>();


   public MovementSurface(final int id) {
      ID = id;
      NAME = String.valueOf(id);
      GROUP = "";
      HAS_CONDITIONAL_COST = false;
      CONDITIONAL_COST_GRID = null;
      LINK = false;
      DEPENDS_ON_ZONE = -1;
      //    IS_ASSOCIATED_FROM_OTHER = false;
   }


   public MovementSurface(final int id,
                          final String name,
                          final String group,
                          final boolean has_conditional_cost,
                          final IRasterLayer conditional_cost_grid,
                          final boolean link,
                          final int depends_on_zone) {

      ID = id;
      NAME = name;
      GROUP = group;
      HAS_CONDITIONAL_COST = has_conditional_cost;
      CONDITIONAL_COST_GRID = conditional_cost_grid;
      LINK = link;
      DEPENDS_ON_ZONE = depends_on_zone;
      //      IS_ASSOCIATED_FROM_OTHER = false;
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
      return ID;
   }


   public String getGroup() {
      return GROUP;
   }


   public boolean hasConditionalCost() {
      return HAS_CONDITIONAL_COST;
   }


   public boolean dependsOn(final int id) {
      if (id == this.DEPENDS_ON_ZONE) {
         return true;
      }
      return false;
   }


   public boolean isDependent() {
      if (this.DEPENDS_ON_ZONE > -1) {
         return true;
      }
      return false;
   }


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

}
