import java.io.*;
/**
 * CS-3251
 * dbclientRTP.java
 * Purpose: Simulates a RTP server with a database
 *
 * @author Brian Eason
 * @author Nico de Leon
 * @author Duc Tran
 * @email brianeason92@gmail.com
 * @email nico@ns.gg
 * @version 1.0 4/20/2016
 */
public class dbengineRTP {
		
	private static String[] IDs;
	private static String[] firstNames;
	private static String[] lastNames;
	private static String[] qualityPoints;
	private static String[] GPAhours;
	private static String[] GPA;
	private final static String databaseName = "3251database-hw1.txt";

	
		public static void main(String[] args) throws IOException{
			if(args.length!=1)
				throw new IllegalArgumentException("Parameters: <Port>");
			
			//populate the database
			IDs = new String[6];
			firstNames = new String[6];
			lastNames = new String[6];
			qualityPoints = new String[6];
			GPAhours = new String[6];
			GPA = new String[6];
			
			int servPort = Integer.parseInt(args[0]);
			
			//create new server socket
			RTPServerSocket servSock = new RTPServerSocket(servPort);
			
			System.out.println(" RTP Server is running");
		
			//begin server socket infinite loop
			for(;;){
				RTPSocket clntSock = servSock.accept();
				DataInputStream in = new DataInputStream(clntSock.getInputStream());
				DataOutputStream out = new DataOutputStream(clntSock.getOutputStream());
				
				//receive message
				String message = in.readUTF();

				//break the message up
				String delim = "[ ]";
				String[] messageArr = message.split(delim);
				
				//begin creating a response
				String response = "From server: ";
				if(checkID(messageArr[1])){
					for(int i = 2; i < messageArr.length; i++){
						if(!checkFields(messageArr[i])){
							response = response + " Error: Field name invalid: " + messageArr[i];
							break;
						}
						else{
							if(i!=messageArr.length-1){
								response = response + " " + fetchInfo(messageArr[i], messageArr[1], false);
							}
							else{
								response = response + " " + fetchInfo(messageArr[i], messageArr[1], true);
							}
						}
					}
				}
				else{
					response = "Database doesn't contain that student ID";
				}
				response = "\n" + response;

				//send out response
				out.writeUTF(response);
				
				try {
				    while (true) {
				        System.out.println(in.readUTF());
				    }
				    } catch (EOFException e) {
				}
				
				clntSock.close();
			}
			/*UNREACHABLE CODE*/
		}
		
		/*
		 * Checks to see if the field is a valid database field
		 * 
		 * @param field, the field in question
		 * @return boolean, true if it's valid, false if not
		 */
		public static boolean checkFields(String field){
			if(field.equals("first_name") || field.equals("last_name") || 
					field.equals("quality_points") || field.equals("gpa_hours") || field.equals("gpa")){
				if(field.equals("gpa") && !field.equals("gpa_hours")){
					if(field.length()>3){
						return false;
					}
					else{
						return true;
					}
				}
				return true;
			}
			return false;
		}

		/*
		 * Gets the information of a student from the database
		 * @param field the attribute needed
		 * @param ID the student's ID
		 * @param last whether or not the field is the last field requested
		 * @return String, the string to be added to the message
		 */
		public static String fetchInfo(String field, String ID, boolean last){
			String response = "";
			if(field.equals("first_name")){
				response = "first_name: " + getFirstName(ID);
			}
			
			else if(field.equals("last_name")){
				response = "last_name: " + getLastName(ID);
			}
			
			else if(field.equals("quality_points")){
				response = "quality_points: " + getQualityPoints(ID);
			}
			
			else if(field.equals("gpa_hours")){
				response = "gpa_hours: " + getGPAHours(ID);
			}
			
			else if(field.equals("gpa")){
				response = "gpa: " + getGPA(ID);
			}
			
			if(last){
				return response;
			}
			else{
				return response + ",";
			}
		}
		
		/*
		 * Checks to see if student ID is valid
		 * 
		 * @param studentID the id in question
		 * @return true if ID is valid, false if not
		 */
		public static boolean checkID(String studentID){
			String line = null;
			try{
				FileReader fileReader = new FileReader(databaseName);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
					int i = 0;
					while((line = bufferedReader.readLine())!=null){
						String delims = "[,]";
						String[] lineArr = line.split(delims);
						int j = 0;
						IDs[i] = lineArr[j]; j++;
						if(IDs[i].equals(studentID)){
							return true;
						}
					}
					bufferedReader.close();
			}
			catch(FileNotFoundException ex){
				System.out.println("Unable to open file");
			}
			catch(IOException ex){
				System.out.println("Error reading file");
			}
			return false;
		}
		
		/*
		 * Grabs the first name from database 
		 * 
		 * @param studentID
		 * @return String first name
		 */
		public static String getFirstName(String studentID){
			String line = null;
			
			try{
				FileReader fileReader = new FileReader(databaseName);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
					int i = 0;
					while((line = bufferedReader.readLine())!=null){
						String delims = "[,]";
						String[] lineArr = line.split(delims);
						int j = 0;
						IDs[i] = lineArr[j]; j++;
						firstNames[i] = lineArr[j]; j++;
						if(studentID.equals(IDs[i])){
							return firstNames[i];
						}
					}
					bufferedReader.close();
			}
			catch(FileNotFoundException ex){
				System.out.println("Unable to open file");
			}
			catch(IOException ex){
				System.out.println("Error reading file");
			}
			return "Error: first name not found";
		}
		
		/*
		 * Grabs the last name from database 
		 * 
		 * @param studentID
		 * @return String last name
		 */
		public static String getLastName(String studentID){
			String line = null;
			
			try{
				FileReader fileReader = new FileReader(databaseName);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
					int i = 0;
					while((line = bufferedReader.readLine())!=null){
						String delims = "[,]";
						String[] lineArr = line.split(delims);
						int j = 0;
						IDs[i] = lineArr[j]; j = j + 2;
						lastNames[i] = lineArr[j];
						if(studentID.equals(IDs[i])){
							return lastNames[i];
						}
					}
					bufferedReader.close();
			}
			catch(FileNotFoundException ex){
				System.out.println("Unable to open file");
			}
			catch(IOException ex){
				System.out.println("Error reading file");
			}
			return "Error: last name not found";
		}
		
		/*
		 * Grabs the quality points from database 
		 * 
		 * @param studentID
		 * @return String quality points
		 */
		public static String getQualityPoints(String studentID){
			String line = null;
			
			try{
				FileReader fileReader = new FileReader(databaseName);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
					int i = 0;
					while((line = bufferedReader.readLine())!=null){
						String delims = "[,]";
						String[] lineArr = line.split(delims);
						int j = 0;
						IDs[i] = lineArr[j]; j = j+3;
						qualityPoints[i] = lineArr[j];
						if (studentID.equals(IDs[i])){
							return qualityPoints[i];
						}
					}
					bufferedReader.close();
			}
			catch(FileNotFoundException ex){
				System.out.println("Unable to open file");
			}
			catch(IOException ex){
				System.out.println("Error reading file");
			}
			
			return "Error: quality points not found";
		}
		
		/*
		 * Grabs the gpa hours from database 
		 * 
		 * @param studentID
		 * @return String gpa hours
		 */
		public static String getGPAHours(String studentID){
			String line = null;
			
			try{
				FileReader fileReader = new FileReader(databaseName);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
					int i = 0;
					while((line = bufferedReader.readLine())!=null){
						String delims = "[,]";
						String[] lineArr = line.split(delims);
						int j = 0;
						IDs[i] = lineArr[j]; j=j+4;
						GPAhours[i] = lineArr[j];
						if(IDs[i].equals(studentID)){
							return GPAhours[i];
						}
						}
					bufferedReader.close();
			}
			catch(FileNotFoundException ex){
				System.out.println("Unable to open file");
			}
			catch(IOException ex){
				System.out.println("Error reading file");
			}
			return "Error: GPA Hours not found";
		}
		
		/*
		 * Grabs the gpa from database 
		 * 
		 * @param studentID
		 * @return String gpa
		 */
		public static String getGPA(String studentID){
			String line = null;
			
			try{
				FileReader fileReader = new FileReader(databaseName);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
					int i = 0;
					while((line = bufferedReader.readLine())!=null){
						String delims = "[,]";
						String[] lineArr = line.split(delims);
						int j = 0;
						IDs[i] = lineArr[j]; j=j+5;
						GPA[i] = lineArr[j];
						if(IDs[i].equals(studentID)){
							return GPA[i];
						}
					}
					bufferedReader.close();
			}
			catch(FileNotFoundException ex){
				System.out.println("Unable to open file");
			}
			catch(IOException ex){
				System.out.println("Error reading file");
			}
			return "Error: GPA not found";
		}
}