package es.udc.cartolab.accesTerrit.utils;

import org.gvsig.fmap.raster.layers.FLyrRasterSE;

public class AreaClass {

    private int clase;
    private String nombre = null;
    private FLyrRasterSE edc;
    private boolean nodo = false;

    public AreaClass(String csv, FLyrRasterSE[] rasters) {
	String[] lines = csv.split("\n");
	String line;
	for (int i = 0; i < lines.length; i++) {
	    line = lines[i];
	    String[] tokens = line.split(";");
	    if (tokens.length >= 2) {
		if (tokens[1].compareTo("clase") == 0) {
		    clase = Integer.parseInt(tokens[2]);
		} else if (tokens[1].compareTo("nombre") == 0) {
		    nombre = tokens[2];
		} else if (tokens[1].compareTo("edc") == 0) {
		    if (tokens.length > 2)
			for (FLyrRasterSE raster : rasters) {
			    if (raster.getName().compareTo(tokens[2]) == 0)
				edc = raster;
			}
		} else if (tokens[1].compareTo("nodo") == 0) {
		    nodo = Boolean.parseBoolean(tokens[2]);
		}
	    }
	}
    }

    public AreaClass(int clase) {
	this.clase = clase;
    }

    /*
     * public AreaClass(int clase, String nombre, String edc, boolean nodo) {
     * this.clase = clase; this.nombre = nombre; this.edc = edc; this.nodo =
     * nodo; }
     * 
     * public AreaClass(String csvLine) { String[] tokens = csvLine.split(";");
     * this.clase = Integer.parseInt(tokens[1]); this.nombre = tokens[2];
     * this.edc = tokens[3]; this.nodo = Boolean.parseBoolean(tokens[4]); }
     */

    public int getClase() {
	return clase;
    }

    public void setClase(int clase) {
	this.clase = clase;
    }

    public String getNombre() {
	return nombre;
    }

    public void setNombre(String nombre) {
	this.nombre = nombre;
    }

    public FLyrRasterSE getEdc() {
	return edc;
    }

    public void setEdc(FLyrRasterSE edc) {
	this.edc = edc;
    }

    public boolean isNodo() {
	return nodo;
    }

    public void setNodo(boolean nodo) {
	this.nodo = nodo;
    }

    public String getCsv() {
	String edcName = (edc == null) ? "" : edc.getName();
	return ";clase;" + clase + "\n;nombre;" + nombre + "\n;edc;" + edcName
		+ "\n;nodo;" + Boolean.toString(nodo) + "\n";
    }

}
