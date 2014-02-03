package es.udc.cartolab.CDC.utils;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import org.gvsig.fmap.raster.layers.FLyrRasterSE;

import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.gvsig.core.gvRasterLayer;
import es.unex.sextante.gvsig.core.gvTable;
import es.unex.sextante.outputs.NullOutputChannel;

public class CDCParameters {

    private FLyrRasterSE origen, zona_despl, scs;
    private Collection<FLyrRasterSE> scc;
    private File destino;
    private Vector<AreaClass> clases = null;
    private Vector<Vector<Boolean>> matriz = null;

    public FLyrRasterSE getOrigen() {
        return origen;
    }

    public void setOrigen(FLyrRasterSE origen) {
        this.origen = origen;
    }

    public FLyrRasterSE getZonaDespl() {
        return zona_despl;
    }

    public void setZonaDespl(FLyrRasterSE zonaDespl) {
        zona_despl = zonaDespl;
    }

    public FLyrRasterSE getScs() {
        return scs;
    }

    public void setScs(FLyrRasterSE scs) {
        this.scs = scs;
    }

    public Collection<FLyrRasterSE> getScc() {
        return scc;
    }

    public void setScc(Collection<FLyrRasterSE> scc) {
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
            for (FLyrRasterSE raster : scc) {
                writer.append(";" + raster.getName() + "\n");
            }
            writer.append("scc\n");

            writer.append("clases\n");
            for (AreaClass clase : clases) {
                writer.append("clase\n" + clase.getCsv() + "clase\n");
            }
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

    public void loadFromCSV(File inputCsv, FLyrRasterSE[] rasters) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputCsv));
            String line = "";
            line = br.readLine();
            lines: while (line != null) {
                String[] tokens = line.split(";");
                if (tokens[0].equals("origen")) {
                    for (FLyrRasterSE raster : rasters) {
                        if (raster.getName().equals(tokens[1])) {
                            origen = raster;
                        }
                    }
                    line = br.readLine();
                } else if (tokens[0].equals("zona_despl")) {
                    for (FLyrRasterSE raster : rasters) {
                        if (raster.getName().equals(tokens[1])) {
                            zona_despl = raster;
                        }
                    }
                    line = br.readLine();
                } else if (tokens[0].equals("destino")) {
                    destino = new File(tokens[1]);
                    line = br.readLine();
                } else if (tokens[0].equals("scs")) {
                    for (FLyrRasterSE raster : rasters) {
                        if (raster.getName().equals(tokens[1])) {
                            scs = raster;
                        }
                    }
                    line = br.readLine();
                } else if (tokens[0].equals("scc")) {
                    scc = new Vector<FLyrRasterSE>();
                    line = br.readLine();
                    if (line == null) {
                        break lines;
                    }
                    tokens = line.split(";");
                    while (!tokens[0].equals("scc")) {
                        for (FLyrRasterSE raster : rasters) {
                            if (raster.getName().equals(tokens[1])) {
                                scc.add(raster);
                            }
                        }
                        line = br.readLine();
                        if (line == null) {
                            break lines;
                        }
                        tokens = line.split(";");
                    }
                    line = br.readLine();
                } else if (tokens[0].equals("clases")) {
                    clases = new Vector<AreaClass>();
                    line = br.readLine();
                    if (line == null) {
                        break lines;
                    }
                    tokens = line.split(";");
                    while (!tokens[0].equals("clases")) {
                        if (tokens[0].equals("clase")) {
                            line = br.readLine();
                            if (line == null) {
                                break lines;
                            }
                            tokens = line.split(";");
                            String areas = "";
                            while (!tokens[0].equals("clase")) {
                                areas += line + "\n";
                                line = br.readLine();
                                if (line == null) {
                                    break lines;
                                }
                                tokens = line.split(";");
                            }
                            clases.add(new AreaClass(areas, rasters));
                            line = br.readLine();
                            if (line == null) {
                                break lines;
                            }
                            tokens = line.split(";");
                        }
                    }
                    line = br.readLine();
                } else if (tokens[0].equals("matriz")) {
                    matriz = new Vector<Vector<Boolean>>();
                    line = br.readLine();
                    if (line == null) {
                        break lines;
                    }
                    tokens = line.split(";");
                    while (!tokens[0].equals("matriz")) {
                        Vector<Boolean> fila = new Vector<Boolean>();
                        for (String token : tokens) {
                            fila.add(Boolean.parseBoolean(token));
                        }
                        matriz.add(fila);
                        line = br.readLine();
                        if (line == null) {
                            break lines;
                        }
                        tokens = line.split(";");
                    }
                    line = br.readLine();
                } else {
                    line = br.readLine();
                }
            }

            if ((clases != null)
                    && (matriz != null)
                    && (matriz.size() > 0)
                    && (clases.size() > 0)
                    && (matriz.get(0).size() > 0)
                    && ((clases.size() != matriz.size()) || (clases.size() != matriz
                            .get(0).size()))) {
                matriz = null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public gvTable getMovementConstraintsTable() {

        gvTable table = new gvTable();

        Class[] types = new Class[matriz.size() + 1];
        types[0] = Integer.class;

        String[] fields = new String[matriz.size() + 1];
        fields[0] = "ZONES";

        for (int i = 1; i <= matriz.size(); i++) {
            types[i] = Boolean.class;
            fields[i] = Integer.toString(i);
        }

        table.create("Movement_constraints", new NullOutputChannel(), types,
                fields);

        for (int i = 0; i < matriz.size(); i++) {
            Object[] record = new Object[matriz.size() + 1];
            record[0] = Integer.toString(i + 1);
            for (int j = 0; j < matriz.get(i).size(); j++) {
                record[j + 1] = matriz.get(i).get(j);
            }
            table.addRecord(record);
        }

        return table;

    }

    public gvTable getMovementSurfaceGroupsTable() {

        gvTable table = new gvTable();

        Class[] types = { Integer.class, String.class, String.class,
                Boolean.class };

        String[] fields = { "Z_Class", "Z_Name", "CSS", "IS_NODE" };

        table.create("Movement_Surfaces_Groups", new NullOutputChannel(),
                types, fields);

        for (int i = 0; i < clases.size(); i++) {
            table.addRecord(clases.get(i).getTableRecord());
        }

        return table;

    }

    public Collection<? extends IRasterLayer> getSccSextante() {
        Iterator<FLyrRasterSE> iter = scc.iterator();
        Collection<gvRasterLayer> rasters = new Vector<gvRasterLayer>();

        while (iter.hasNext()) {
            gvRasterLayer raster = new gvRasterLayer();
            raster.create(iter.next());
            raster.open();
            rasters.add(raster);
        }

        return rasters;
    }
    
    public FLyrRasterSE checkExtents() {
    	Rectangle2D originExtent = origen.getFullExtent();
    	if (!originExtent.equals(scs.getFullExtent())) {
    		return scs;
    	}
    	if (!originExtent.equals(zona_despl.getFullExtent())) {
    		return zona_despl;
    	}
    	for (FLyrRasterSE raster:this.scc) {
        	if (!originExtent.equals(raster.getFullExtent())) {
        		return raster;
        	}
    	}
    	return null;
    }
    
    public boolean checkUniqueRasters() {
        HashSet<FLyrRasterSE> set = new HashSet<FLyrRasterSE>();
        boolean unique = true;
        unique &= set.add(origen);
        unique &= set.add(zona_despl);
        unique &= set.add(scs);
        for (FLyrRasterSE raster : scc) {
            unique &= set.add(raster);
        }
        return unique;
    }
}
