package cloudinterface;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;
import gui.DebugMessage;
import gui.WCAFrame;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CloudInterface {
    
    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();    
    static final JsonFactory JSON_FACTORY = new JacksonFactory();    
    static DebugMessage debugmsg;
    
    static final String URL_BASE = "http://129.59.107.182";
    static final String USERNAME = "isstac";
    static final String PASSWORD = ""; // get PW from Dmitriy

    // all message types written to debug text field are the CLOUD type
    static final String EventType = "CLOUD";
        
    // (Note: None is used if there are no parameters)
    public enum CloudFileType {
        outputs, inputs;
    }

    HttpRequestFactory requestFactory = 
        HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) {
                request.setParser(new JsonObjectParser(JSON_FACTORY));
            }
        });

    public CloudInterface(DebugMessage dbgmsg) {        
        debugmsg = dbgmsg;
        debugmsg.setTypeColor(EventType, gui.Util.TextColor.Gold, gui.Util.FontType.BoldItalic);
    }
    
    public static class SubmissionUrl extends GenericUrl {
        public SubmissionUrl(String encodedUrl) {
            super(encodedUrl);
    }

        @Key
        public String fields;
    }
    
    public static class Job implements Comparable<Job> {
        @Key
        public String id;
        
        @Key
        public String name;
        
        @Key
        public String user;
        
        @Key
        public String queue;
        
        @Key
        public String state;
        
        @Key
        public String finalStatus;
        
        @Key
        public double progress;
        
        @Key
        public long startedTime;
        
        @Key
        public long finishedTime;
        
        @Override
        public int compareTo(Job other) {
            return id.compareTo(other.id);
        }
    }
    
    public static class JobOutput {        
        @Key
        public long accessTime;
        
        @Key
        public int blockSize;
        
        @Key
        public int childrenNum;
        
        @Key
        public int fileId;
        
        @Key
        public int length;
        
        @Key
        public long modificationTime;
        
        @Key
        public String pathSuffix;     
    }
    
    private void displayCloudEvent (String method, String message) {
        debugmsg.print(EventType, "[" + method + "] - " + message);
    }
    
    /**
     * returns the results for the specified job id and output name.
     * 
     * @param appId - the job id
     * @param resultType - the type of file (inputs or outputs)
     * @param fileName - the name of the output file as returned by getJobOutputs()
     * 
     * @return the contents of the specified file
     */
    public ByteArrayOutputStream getJobFile(String appId, CloudFileType resultType, String fileName) {        
        String resource = URL_BASE + ":8808/cloud/application/" + appId +  "/" + resultType + "/" + fileName;  //fileName = results.json
        GenericUrl url = new GenericUrl(resource);
        displayCloudEvent("getJobFile", url.toString());
        
        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            request.getHeaders().setBasicAuthentication(USERNAME, PASSWORD);

            HttpResponse response = request.execute();
            int responseCode = response.getStatusCode();
            
            if (responseCode == 200) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                response.download(baos);
                return baos;
            } else {
                return null;
            }            
        } catch (IOException e) {
            System.err.println("Error building request: " + e.getMessage());
        }                
        
        return null;
    }
    
    /**
     * returns the results for the specified job id and output name.
     * 
     * @param appId - the job id
     * @param resultType - the type of file (inputs or outputs)
     * @param fileName - the name of the output file as returned by getJobOutputs()
     * 
     * @return the contents of the specified file
     */
    public String getMCTSResult(String appId, String resultType, String fileName) {        
        String resource = URL_BASE + ":8808/cloud/application/" + appId +  "/" + resultType + "/" + fileName;  //fileName = results.json             
        GenericUrl url = new GenericUrl(resource);
        displayCloudEvent("getMCTSResult", url.toString());
        
        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            request.getHeaders().setBasicAuthentication(USERNAME, PASSWORD);

            HttpResponse response = request.execute();
            int responseCode = response.getStatusCode();
            
            if (responseCode == 200) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                response.download(baos);
                return baos.toString();
            } else {
                return null;
            }            
        } catch (IOException e) {
            System.err.println("Error building request: " + e.getMessage());
        }                
        
        return null;
    }
    
    /**
     * returns the output files produced for the specified job id.
     * 
     * @param appId - the job id
     * @param type - the type of file (inputs or outputs)
     * 
     * @return the list of files associated with the specified job
     */
    public JobOutput[] getJobFileList(String appId, CloudFileType type) {        
        String resource = URL_BASE + ":8808/cloud/application/" + appId +  "/" + type.toString() + "/list";        
        GenericUrl url = new GenericUrl(resource);
        displayCloudEvent("getJobFileList", url.toString());
        JobOutput[] jobs = new JobOutput[0];
                
        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            request.getHeaders().setBasicAuthentication(USERNAME, PASSWORD);
            jobs = request.execute().parseAs(JobOutput[].class);
        } catch (IOException e) {
            System.err.println("Error with request: " + e.getMessage());
        }                
        
        return jobs;
    }        

    /**
     * returns the list of jobs that have run or are running in the cloud.
     * 
     * @return an array of jobs that are currently posted
     */    
    public Job[] getJobs() {
        String resource = URL_BASE + ":8808/cloud/applications/list";        
        GenericUrl url = new GenericUrl(resource);
        displayCloudEvent("getJobs", url.toString());
        Job[] jobs;
        
        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            request.getHeaders().setBasicAuthentication(USERNAME, PASSWORD);
            jobs = request.execute().parseAs(Job[].class);                                  
        } catch (IOException e) {
            System.err.println("Error building request: " + e.getMessage());
            return null;
        }                
        
        return jobs;
    }
    
    /**
     * posts a job to run in the cloud.
     * 
     * @param archive - the zip file containing the required jars and the jpf config file
     * @param numExecutors - the number of executors to use in the exploration
     * @param partitionSize - the 
     * @param jpfFileName - the jpf config file to use (must be included in zip file)
     * 
     * @return the jobid assigned to the cloud job (empty if failure)
     */
    public String postJob(File archive, String numExecutors, String partitionSize, String jpfFileName) {
        // make sure inputs are valid
        if (jpfFileName == null || jpfFileName.isEmpty())
            return null;
            
        // 1. Make the multipart/form-data 
        MultipartContent content = new MultipartContent().setMediaType(
        new HttpMediaType("multipart/form-data")
                .setParameter("boundary", "__END_OF_PART__"));
        
        // 2. Add the zip archive
        MultipartContent.Part part1 = new MultipartContent.Part(new FileContent("application/zip", archive));        
        part1.setHeaders(new HttpHeaders().set(
            "Content-Disposition", 
            String.format("form-data; name=\"archive\"; filename=\"%s\"", archive.getName())));                
        content.addPart(part1);
        
        // 3. Add the parameters
        Map<String, String> parameters = new HashMap<>();        
        if (numExecutors != null && !numExecutors.isEmpty())
            parameters.put("numberExecutors", numExecutors);
        if (partitionSize != null && !partitionSize.isEmpty())
            parameters.put("partitionSize", partitionSize);
        parameters.put("jpfFileName", jpfFileName);
                
        for (String name : parameters.keySet()) {
            MultipartContent.Part part = new MultipartContent.Part(
                    new ByteArrayContent("text/plain", parameters.get(name).getBytes()));
            part.setHeaders(new HttpHeaders().set(
                    "Content-Disposition", String.format("form-data; name=\"%s\"", name)));
            content.addPart(part);
        }
        
        SubmissionUrl url = new SubmissionUrl(URL_BASE + ":8808/cloud/sampling_spf"); // sampling is here?
        displayCloudEvent("postJob", url.toString());
        
        String response = "";
        
        try {
            HttpRequest request = requestFactory.buildPostRequest(url, content);
            request.getHeaders().setBasicAuthentication(USERNAME, PASSWORD);
            response = request.execute().parseAsString();
            System.out.println(response);
        } catch (IOException e) {
            System.err.println("Error building request: " + e.getMessage());
        }
        
        return response;
    }
}