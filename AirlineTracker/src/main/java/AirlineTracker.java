import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class AirlineTracker extends JFrame{
	private static int counter = 0;
    private static final String APPLICATION_NAME = "Airline Tracker";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    
    public static JLabel destinationLbl = new JLabel("Destination:");
    public static JLabel dateLbl = new JLabel("Date:");
    public static JTextField destinationTxt = new JTextField(3);
    public static JTextField dateTxt = new JTextField(5);
    public static JButton trackFlightBtn = new JButton("Track Flights");
    public static JButton exitBtn = new JButton("Exit");
    public static JPanel gui = new JPanel();
    public static JPanel labelsP1 = new JPanel();
    public static JPanel labelsP2 = new JPanel();
    public static JPanel buttonsP = new JPanel();
    public static Timer timer = new Timer(25000, new TimerListener());
    
    
    
    
    public AirlineTracker(String title) {
    	super(title);
    	labelsP1.add(destinationLbl); labelsP1.add(destinationTxt);
    	labelsP2.add(dateLbl); labelsP2.add(dateTxt);
    	buttonsP.add(trackFlightBtn); buttonsP.add(exitBtn);
    	gui.setLayout(new BoxLayout(gui, BoxLayout.Y_AXIS));
    	gui.add(labelsP1);gui.add(labelsP2); gui.add(buttonsP);
    	add(gui, BorderLayout.NORTH);
    	trackFlightBtn.addActionListener(new ActionListener(){
	    		public void actionPerformed(ActionEvent e){
	    		if (trackFlightBtn.getLabel().compareTo("Track Flights") == 0) {
	    			trackFlightBtn.setLabel("Stop");
	    			try {
	        			trackFlight();
	        		}
	        			catch(IOException exception){
	        				exception.printStackTrace();
	        			}
	        			catch (GeneralSecurityException g){
	        				g.printStackTrace();
	        			}
	    			timer.start();
	    		}
	    		else {
	    			timer.stop();
	    			trackFlightBtn.setLabel("Track Flights");
	    		}
    		}
    	});

    	exitBtn.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				System.exit(0);
			}
		});
    }
    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    //private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "\\credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials() throws IOException, GeneralSecurityException {
        // Load client secrets.
        InputStream in = AirlineTracker.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS);
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }
    public static Sheets getSheetsService() throws IOException, GeneralSecurityException{
    	Credential credential = getCredentials();
    	return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(),
    			JSON_FACTORY, credential)
    			.setApplicationName("Airline Tracker")
    			.build();
    }
    /**
     * 
     */
    public static enum months {JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC};
    public static int[] days = {31,28,31,30,31,30,31,31,30,31,30,31};

    public static int checkDate(int hourInb, String monthInb, String dayInb, int hourKe, String monthKe, String dayKe, String airport, String destination) {
    	int monthINB = (months.valueOf(monthInb).ordinal()+1); //actual month inb flight
    	int monthKE = (months.valueOf(monthKe).ordinal()+1); //actual month ke flight
    	int dayKE = Integer.parseInt(dayKe); //actual day ke flight
    	int dayINB = Integer.parseInt(dayInb);
    	System.out.print("keM"+monthKE+" keD:"+dayKE+" hourKe:"+hourKe+
    			"\ninbM:"+monthINB+" inbD:"+dayInb+" hourInb:"+hourInb+
    			"\n"+airport + "=?=" +destination.toUpperCase()+" ");
    	if (airport.compareTo(destination.toUpperCase()) == 0){ //not the same airport code or track all
    	if (hourKe < hourInb) {//if true then flight could have flew day before
    		if (monthINB == monthKE //same month?
    				|| monthINB == (monthKE-1) //month before?
    				|| monthINB == 1 && monthKE == 12){//new year case
    			if(dayINB == (dayKE-1) //day before in same month
    					|| (dayINB == days[monthINB-1] && dayKE == 1)){//inb flight last day of month, ke flight first day of month 
    				return 1;//inbound landed day before flight
    			}
    		}
    	} else if (monthINB == monthKE && dayINB == dayKE) {
    			return 0;//inbound landed day of flight
    		}
    	}
    	
    	return -1; //not the flight
    }
    public static void main(String[] args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        AirlineTracker frame = new AirlineTracker("Airline Tracker");
        frame.setSize(480,480);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setVisible(true);
    }
    public static void trackFlight() throws IOException, GeneralSecurityException {
    	final String spreadsheetId = "156Fm_ktpuNwmOp5d1I8KEFhxHTaLHxvVmonDpd1r34k";
    	
        String range = "FlightTracker!A2:A";
        String keUrl = "https://www.google.com/search?client=firefox-b-1-d&q=flight+ke+92";
        String destination = destinationTxt.getText();
        Document keDocument = Jsoup.connect(keUrl).get();
        Elements keDays = keDocument.getElementsByClass("TnUzCf");
        Elements keScheduleTime = keDocument.select("div.kBrfsc");
        String[] keDateInfo = new String[10];
        String[] keSchedTime = new String[10];
        String[] nowDate = {"","",""};
        int keHour = 0;
        if (java.time.LocalDate.now().getYear()%4 == 0){//leap year
    		days[1] = 29;
    	}
        if (dateTxt.getText() == ""){
        	nowDate =  java.time.LocalDate.now().toString().split("-");//YYYY-MM-DD 0:YYYY, 1:MM, 2:DD
        }
        else {
        	nowDate[0] = String.valueOf(java.time.LocalDate.now().getYear());
        	nowDate[2] = dateTxt.getText().substring(0,2);
        	nowDate[1] = String.valueOf(months.valueOf(dateTxt.getText().substring(2).toUpperCase()).ordinal()+1);
        }
        System.out.println("Today's Date: "+java.time.LocalDate.now().toString());
        int i = 0;
        for (Element day : keDays) {
        	keDateInfo = day.text().split(" ");//Thu, January 2
        	keDateInfo[1] = keDateInfo[1].substring(0,3).toUpperCase(); //[JAN]uary
        	System.out.println(keDateInfo[1]);
      		if ((months.valueOf(keDateInfo[1]).ordinal()+1) == Integer.parseInt(nowDate[1])
      			&& Integer.parseInt(keDateInfo[2])==Integer.parseInt(nowDate[2])) { //compare
      			System.out.println("keSchedule"+keScheduleTime.get(i).text().toString());
       			keSchedTime = keScheduleTime.get(i).text().split(" "); //0:depTime; 1:am/pm; 2:arrTime; 3:am/pm
      			keHour = Integer.parseInt(keSchedTime[0].split(":")[0]);
      			if (keHour < 12 && keSchedTime[1].compareTo("pm") == 0){
      				keHour += 12;
      			}
      			break;
      		}
      		else i++;
        }
        System.out.println("Flight of ke: "+keDateInfo[1]+" "+keDateInfo[2]+" "+keHour+"\n\n");
        i = 0;
        Sheets service = getSheetsService();
        
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> values = response.getValues();
        //System.out.println(values.size());
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
        	List<List<Object>> inputValue = Arrays.asList(Arrays.asList());
        	int rowOperations = 2;
            System.out.println("Flight Numbers");
            for (List flight : values) {
            	if (flight.isEmpty() || flight.get(0) == "") flight = values.get(values.indexOf(flight)+1);
            	String airlineCode = flight.get(0).toString().substring(0,2);
            	String flightNumber = flight.get(0).toString().substring(2);
            	String url = "https://www.google.com/search?client=firefox-b-1-d&q=flight+"
            	+airlineCode.toLowerCase()+"+"+flightNumber;
            	System.out.println(airlineCode+" "+flightNumber);
            	Document document = Jsoup.connect(url).get();
            	Elements days = document.getElementsByClass("TnUzCf");
            	Elements scheduleTime = document.select("div.kBrfsc");
                Elements arrival = document.select("tr.kWOKDd:nth-child(3)");
                Elements airPorts = document.select("table.ts:nth-child(2)");
                Elements statUs = document.select("span.l1P94c");
                boolean foundFlight = false;
                String[] dateInfo = new String[10];
                String[] schedTime = new String[10];
                
                String[] arrivalInfo = new String[10];
                String[] airports = new String[10];
                String status="";
                int hour = 0;
                i = 0;
                System.out.println("days size: "+days.size()+", "+"airPorts size: "+airPorts.size());
                for (Element day : days){
                	if (airPorts.size() == days.size() * 2){
                		for (int j = i; j <= (i+1)*2-1; j++){
                			System.out.print(j+": ");
                			airports = airPorts.get(j).text().split(" ");//0:departure code, 1:arrival code
                			schedTime = scheduleTime.get(j).text().split(" ");
                			airports = airPorts.get(j).text().split(" ");
                        	schedTime = scheduleTime.get(j).text().split(" "); //0:depTime; 1:am/pm; 2:arrTime; 3:am/pm
                        	hour = Integer.parseInt(schedTime[2].split(":")[0]);//split arrTime take hour
                    		if (hour < 12 && schedTime[1].compareTo("pm") == 0){
                    			hour += 12;
                    		}
                    		dateInfo = day.text().split(" ");//Thu, January 2
                    		dateInfo[1] = dateInfo[1].substring(0,3).toUpperCase();//[JAN]uary
                    		int dateChecked = 
                    				checkDate(hour, dateInfo[1], dateInfo[2], keHour, keDateInfo[1], keDateInfo[2], airports[1], destination);
                    		if ( dateChecked == 1 || dateChecked == 0) {
                    			//Original KE schedule less than original inb airline use prev day inb flight
                      			status = statUs.get(j).text();
                      			arrivalInfo = arrival.get(j).text().split(" ");
                      			foundFlight = true;
                      			System.out.print("dateCheck = "+dateChecked + " ");
                      			break;
                    		}
                    		else {
                    			System.out.println("dateCheck = "+dateChecked + " Not found");
                    		}
                		}
                		if (foundFlight) {
                			inputValue = Arrays.asList(Arrays.asList(airports[0], arrivalInfo[0]+arrivalInfo[1],
                					airports[1],arrivalInfo[4]+arrivalInfo[5],
                					status, dateInfo[1]+" "+dateInfo[2]));
                			System.out.println("Yes found");
                			break;
                		}
                		i+=2;
                	} else {//if no two flights in a day
                		System.out.print(i+": ");
                		airports = airPorts.get(i).text().split(" ");
                		schedTime = scheduleTime.get(i).text().split(" "); //0:depTime; 1:am/pm; 2:arrTime; 3:am/pm
                		hour = Integer.parseInt(schedTime[2].split(":")[0]);//split arrTime take hour
                		if (hour < 12 && schedTime[1].compareTo("pm") == 0){
                			hour += 12;
                		}
                		dateInfo = day.text().split(" ");//Thu, January 2
                		dateInfo[1] = dateInfo[1].substring(0,3).toUpperCase();//[JAN]uary
                		int dateChecked = 
                				checkDate(hour, dateInfo[1], dateInfo[2], keHour, keDateInfo[1], keDateInfo[2], airports[1], destination);
                		if ( dateChecked == 1 || dateChecked == 0) {
                			//Original KE schedule less than original inb airline use prev day inb flight
                			status = statUs.get(i).text();
                			arrivalInfo = arrival.get(i).text().split(" ");
                			foundFlight = true;
                			System.out.print("dateCheck = "+dateChecked + " ");
                		}
                		if (foundFlight) {
                			inputValue = Arrays.asList(Arrays.asList(airports[0], arrivalInfo[0]+arrivalInfo[1],
                					airports[1],arrivalInfo[4]+arrivalInfo[5],
                					status, dateInfo[1]+" "+dateInfo[2]));
                			System.out.println("Yes found");
                			break;
                		}
                		System.out.println("dateCheck = "+dateChecked + " Not found");
                		i++;
                		
                	}
                }
            	if (!foundFlight) {
            		inputValue=Arrays.asList(
                		Arrays.asList("NO FLIGHT","NO FLIGHT","NO FLIGHT","NO FLIGHT","NO FLIGHT","NO FLIGHT")
                				);
            	}
                //List<List<Object>> testValue = Arrays.asList(Arrays.asList("this","is","only","a","test"));
                ValueRange body = new ValueRange()
                		.setValues(inputValue);
                range = "FlightTracker!B"+String.valueOf(rowOperations)+":G"+String.valueOf(rowOperations);
                rowOperations++;
                //System.out.println(range);
                service.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
               .execute();
            }
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");  
            Date date = new Date();  
            System.out.println("Last update: "+formatter.format(date));
            range = "FlightTracker!J1";
            List<List<Object>> lastTimeUpdate = Arrays.asList(Arrays.asList(formatter.format(date)));
            ValueRange body = new ValueRange()
            		.setValues(lastTimeUpdate);
            service.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("RAW")
           .execute();
        }
        String rateRange = "FlightTracker!J2";
        ValueRange refreshRange = service.spreadsheets().values()
        		.get(spreadsheetId, rateRange)
        		.execute();
        List<List<Object>> refreshTime = refreshRange.getValues();
        if (refreshTime == null || refreshTime.isEmpty()){
        	System.out.println("No data found");
        } else {
        	int cycle = Integer.parseInt(refreshTime.get(0).get(0).toString().split(" ")[0])*60000;
        	timer.stop();
        	timer = new Timer(cycle, new TimerListener());
        	timer.start();
        	System.out.println(cycle);
        }
    }
    static class TimerListener implements ActionListener {
    	public void actionPerformed(ActionEvent e){
    		try {
    			trackFlight();
    		}
    			catch(IOException exception){
    				exception.printStackTrace();
    			}
    			catch (GeneralSecurityException g){
    				g.printStackTrace();
    			}
 
    		System.out.println(++counter);
    	}
    }
}