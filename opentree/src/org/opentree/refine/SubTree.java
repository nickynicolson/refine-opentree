package org.opentree.refine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.commands.Command;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class SubTree extends Command {
    protected RowVisitor createRowVisitor(Project project, int cellIndex, List<Long> values) throws Exception {
        return new RowVisitor() {
            int cellIndex;
            List<Long> values;
            
            public RowVisitor init(int cellIndex, List<Long> values) {
                this.cellIndex = cellIndex;
                this.values = values;
                return this;
            }
            
            @Override
            public void start(Project project) {
            	// nothing to do
            }
            
            @Override
            public void end(Project project) {
            	// nothing to do
            }
            
            public boolean visit(Project project, int rowIndex, Row row) {
                try {
                    Long val = null;
                    Object value = row.getCellValue(this.cellIndex);
                    if (value instanceof Integer) {
                        val = ((Integer) value).longValue();
                    } else if (value instanceof String) {
                        val = Long.parseLong((String) value);
                    } else {
                        val = (Long) value;
                    }
                    this.values.add(val);
                } catch (Exception e) {
                	System.out.println("Error in getting value from index[" + this.cellIndex + "], value: " + row.getCellValue(this.cellIndex));
                	e.printStackTrace();
                }

                return false;
            }
        }.init(cellIndex, values);
    }


    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    };
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {            
        try {
            ProjectManager.singleton.setBusy(true);
            Project project = getProject(request);
            ColumnModel columnModel = project.columnModel;
            Column column = columnModel.getColumnByName(request.getParameter("column_name"));
            int cellIndex = column.getCellIndex();

            List<Long> values = new ArrayList<Long>();

            Engine engine = new Engine(project);
            JSONObject engineConfig = null;

            try {
                engineConfig = ParsingUtilities.evaluateJsonStringToObject(request.getParameter("engine"));
            } catch (JSONException e) {
                // ignore
            }

            engine.initializeFromJSON(engineConfig);

            FilteredRows filteredRows = engine.getAllFilteredRows();
            filteredRows.accept(project, createRowVisitor(project, cellIndex, values));
            
            HashMap map = computeSubtree(values);
            JSONWriter writer = new JSONWriter(response.getWriter());

            writer.object();

            for (Iterator<Map.Entry> entries = map.entrySet().iterator(); entries.hasNext();) {
                Map.Entry entry = entries.next();
                writer.key(entry.getKey().toString());
                writer.value(entry.getValue().toString());
            }

            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    };

    public HashMap computeSubtree(List<Long> values) {
        HashMap map = new HashMap();
        HashMap<Float, Integer> modeMap = new HashMap<Float, Integer>();

        // Send the ottids to the web service to get an induced subtree:
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://api.opentreeoflife.org/v2/tree_of_life/induced_subtree");
        try {
          List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
          String ids = "{\"ott_ids\":" + Arrays.toString(values.toArray()) + "}";
          System.out.println(ids);
          post.setEntity(new StringEntity(ids));
          StringBuffer sb = new StringBuffer();
          HttpResponse response = client.execute(post);
          BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
          String line = "";
          while ((line = rd.readLine()) != null) {
            sb.append(line);
          }
          String jsonResponse = sb.toString();
          System.out.println(jsonResponse);
          org.json.JSONObject o = new JSONObject(jsonResponse);
          map.put("newick", o.get("subtree"));
        } catch (Exception e) {
          e.printStackTrace();
        }
        
        map.put("ottIds", Arrays.toString(values.toArray()));
        return map;
    }
}