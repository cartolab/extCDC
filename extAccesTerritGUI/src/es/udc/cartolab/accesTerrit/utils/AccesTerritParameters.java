package es.udc.cartolab.accesTerrit.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import es.unex.sextante.dataObjects.IRasterLayer;

public class AccesTerritParameters {

    private IRasterLayer origen, zona_despl, scs;
    private Collection<IRasterLayer> scc;
    private File destino;
    private Vector<AreaClass> clases = null;
    private Vector<Vector<Boolean>> matriz = null;

    public IRasterLayer getOrigen() {
	return origen;
    }

    public void setOrigen(IRasterLayer origen) {
	this.origen = origen;
    }

    public IRasterLayer getZonaDespl() {
	return zona_despl;
    }

    public void setZonaDespl(IRasterLayer zonaDespl) {
	zona_despl = zonaDespl;
    }

    public IRasterLayer getScs() {
	return scs;
    }

    public void setScs(IRasterLayer scs) {
	this.scs = scs;
    }

    public Collection<IRasterLayer> getScc() {
	return scc;
    }

    public void setScc(Collection<IRasterLayer> scc) {
	this.scc = scc;
    }

    public File getDestino() {
	return destino;
    }

    public void setDestino(File destino) {
	this.destino = destino;
    }

    public Vector<AreaClass> getClasses() {
	return clases;
    }

    public void setClases(Vector<AreaClass> clases) {
	this.clases = clases;
    }

    public Vector<Vector<Boolean>> getMatriz() {
	return matriz;
    }

    public void setMatriz(Vector<Vector<Boolean>> matriz) {
	this.matriz = matriz;
    }

    public void initializeClasses(Integer[] classes) {
	this.clases = new Vector<AreaClass>();
	for (Integer i : classes) {
	    AreaClass clase = new AreaClass(i);
	    this.clases.add(clase);
	}
    }

    public void writeToCSV(File outputCsv) {
	try {

	    FileWriter writer = new FileWriter(outputCsv);

	    writer.append("origen;" + origen.getName() + "\n");
	    writer.append("zona_despl;" + zona_despl.getName() + "\n");
	    writer.append("scs;" + scs.getName() + "\n");
	    writer.append("destino;" + destino.getAbsolutePath() + "\n");
	    writer.append("scc\n");
	    for (IRasterLayer raster : scc) {
		writer.append(";" + raster.getName() + "\n");
	    }
	    writer.append("scc\n");

	    writer.append("clases\n");
	    for (AreaClass clase : clases)
		writer.append("clase\n" + clase.getCsv() + "clase\n");
	    writer.append("clases\n");

	    writer.append("matriz\n");
	    for (Vector<Boolean> fila : matriz) {
		for (Boolean celda : fila) {
		    writer.append(celda.toString() + ";");
		}
		writer.append("\n");
	    }
	    writer.append("matriz\n");

	    writer.flush();
	    writer.close();

	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public void loadFromCSV(File inputCsv, IRasterLayer[] rasters) {
	try {
	    BufferedReader br = new BufferedReader(new FileReader(inputCsv));
	    String line = "";
	    line = br.readLine();
	    while (line != null) {
		String[] tokens = line.split(";");
		if (tokens[0].equals("origen")) {
		    for (IRasterLayer raster : rasters) {
			if (raster.getName().equals(tokens[1]))
			    origen = raster;
		    }
		    line = br.readLine();
		} else if (tokens[0].equals("zona_despl")) {
		    for (IRasterLayer raster : rasters) {
			if (raster.getName().equals(tokens[1]))
			    zona_despl = raster;
		    }
		    line = br.readLine();
		    if (line == null)
			return;
		} else if (tokens[0].equals("destino")) {
		    destino = new File(tokens[1]);
		    line = br.readLine();
		    if (line == null)
			return;
		} else if (tokens[0].equals("scs")) {
		    for (IRasterLayer raster : rasters) {
			if (raster.getName().equals(tokens[1]))
			    scs = raster;
		    }
		    line = br.readLine();
		    if (line == null)
			return;
		} else if (tokens[0].equals("scc")) {
		    scc = new Vector<IRasterLayer>();
		    line = br.readLine();
		    if (line == null)
			return;
		    tokens = line.split(";");
		    while (!tokens[0].equals("scc")) {
			for (IRasterLayer raster : rasters) {
			    if (raster.getName().equals(tokens[1]))
				scc.add(raster);
			}
			line = br.readLine();
			if (line == null)
			    return;
			tokens = line.split(";");
		    }
		    line = br.readLine();
		    if (line == null)
			return;
		} else if (tokens[0].equals("clases")) {
		    clases = new Vector<AreaClass>();
		    line = br.readLine();
		    if (line == null)
			return;
		    tokens = line.split(";");
		    while (!tokens[0].equals("clases")) {
			if (tokens[0].equals("clase")) {
			    line = br.readLine();
			    if (line == null)
				return;
			    tokens = line.split(";");
			    String areas = "";
			    while (!tokens[0].equals("clase")) {
				areas += line + "\n";
				line = br.readLine();
				if (line == null)
				    return;
				tokens = line.split(";");
			    }
			    clases.add(new AreaClass(areas, rasters));
			    line = br.readLine();
			    if (line == null)
				return;
			    tokens = line.split(";");
			}
		    }
		    line = br.readLine();
		    if (line == null)
			return;
		} else if (tokens[0].equals("matriz")) {
		    matriz = new Vector<Vector<Boolean>>();
		    line = br.readLine();
		    if (line == null)
			return;
		    tokens = line.split(";");
		    while (!tokens[0].equals("matriz")) {
			Vector<Boolean> fila = new Vector<Boolean>();
			for (String token : tokens) {
			    fila.add(Boolean.parseBoolean(token));
			}
			matriz.add(fila);
			line = br.readLine();
			if (line == null)
			    return;
			tokens = line.split(";");
		    }
		    line = br.readLine();
		    if (line == null)
			return;
		} else {
		    line = br.readLine();
		    if (line == null)
			return;
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}
