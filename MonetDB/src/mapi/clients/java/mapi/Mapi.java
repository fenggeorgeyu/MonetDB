/* 
 * The contents of this file are subject to the MonetDB Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at 
 * http://monetdb.cwi.nl/Legal/MonetDBPL-1.0.html
 * 
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * The Original Code is the Monet Database System.
 * 
 * The Initial Developer of the Original Code is CWI.
 * Portions created by CWI are Copyright (C) 1997-2002 CWI.  
 * All Rights Reserved.
 * 
 * Contributor(s): Martin Kersten <Martin.Kersten@cwi.nl>
 *		   Peter Boncz <Peter.Boncz@cwi.nl>
 *		   Niels Nes <Niels.Nes@cwi.nl>
 *		   Stefan Manegold  <Stefan.Manegold@cwi.nl>

The Monet Application Programming Interface
Author M.L. Kersten

The easiest way to extend the functionality of Monet is to construct
an independent Monet application, which communicates with a
running server on a textual basis. 
The prime advantage is a secure and debugable interface. 
The overhead in command preparation and response decoding is mainly 
localized in the workstation of the application.

The Java code base presented here is a 'copy' of the similar C-Mapi interface.
The main differences are that this version does not provide variable
binding to ease interaction.
It is not possible to bind source/destination variables.
Instead, column values should be explicitly extracted from the row cache
and arguments are passed upon query construction.
Furthermore, it uses a simple line-based protocol with the server.

See the corresponding documentation. A synopsis is given only.

bind()     	&       Bind string C-variable to field	 [NON_JAVA]
bind_var() 	&       Bind typed C-variable to field [NON_JAVA]
bind_numeric()	& 	Bind numeric C-variable to field [NON_JAVA]
cacheLimit()	& 	Set the tuple cache limit 
cacheShuffle()	& 	Set the cache shuffle percentage 
cacheFreeup()	&	Empty a fraction of the cache
connect()  	&       Connect to a Mserver 
connectSSL()	& 	Connect to a Mserver using SSL [TODO]
disconnect()	& 	Disconnect from server
dup()      	&       Duplicate the connection structure
explain()  	&       Display error message and context on stream
execute()  	&       Execute a query
executeArray()	&	Execute a query using string arguments
fetchField()	&	Fetch a field from the current row
fetchFieldArray()        & Fetch all fields from the current row
fetchLine()	&       Retrieve the next line
fetchReset()	&       Set the cache reader to the begining
fetchRow()	&       Fetch row of values
fetchAllRows()	&       Fetch all answers from server into cache
finish()	&       Terminate the current query
getHost() 	&       Host name of server
getLanguage()	& 	Query language name
getMapiVersion()	& 	Mapi version identifier
getMonetVersion()	& 	Monet version identifier
getMonetId()	& 	Monet version generation number
getUser() 	&       Get current user name
getDBname() 	&       Get current database name
getTable()    	&       Get current table name
getColumnCount()& 	Number of columns in current row
getColumnName()	& 	Get columns name
getRowCount() 	&       Number of lines in cache or -1
getTupleCount()	&       Number of tuples in cache or -1
gotError()    	&       Test for error occurrence
getPort()	&	Connection IP number
ping()     	&       Test server for accessibility
prepareQuery() 	&       Prepare a query for execution
prepareQueryArray()&       Prepare a query for execution using arguments
query()    	&       Send a query for execution
queryArray()	& 	Send a query for execution with arguments
quickQuery()	&       Send a query for execution
quickQueryArray()        & Send a query for execution with arguments
quote()    	& 	Escape characters
reconnect()	&	Restart with a clean session
rows_affected() & 	Obtain number of rows changed
quickResponse()	&       Quick pass response to stream
initStream()	&	Prepare for reading a stream of answers
seekRow() 	&       Move row reader to specific location in cache
timeout()  	&       Set timeout for long-running queries[TODO]
trace()    	&       Set trace flag
traceLog()	& 	Keep log of interaction
unquote()  	&       remove escaped characters

/*
 * The contents of this file are subject to the Monet Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at 
 * http://monetdb.cwi.nl/Legal/MonetDBLicense-1.0.html
 * 
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * The Original Code is the Monet Database System.
 * 
 * The Initial Developer of the Original Code is CWI.
 * Portions created by CWI are Copyright (C) 1997-2003 CWI.  
 * All Rights Reserved.
 * 
 * Contributor(s): Martin Kersten <Martin.Kersten@cwi.nl>
 */

package mapi;

import java.awt.*;
import java.applet.Applet;
import java.applet.*;
import java.io.*;
import java.net.Socket;

public class Mapi 
{

	public final static int MOK = 0;
	public final static int MERROR = -1;
	public final static int MEOS = -1;
	public final static int MAPIERROR  = -4;
	public final static char PROMPTBEG= (char) 1;
	public final static char PROMPTEND= (char) 2;

        static final int MAPIBLKSIZE = 1024;


//	public final int SQL_AUTO        =0;      /* automatic type detection */
//	public final int SQL_INT         =1;
//	public final int SQL_LONG        =2;
//	public final int SQL_CHAR        =3;
//	public final int SQL_VARCHAR     =4;
//	public final int SQL_FLOAT       =5;
//	public final int SQL_DOUBLE      =6;
//	public final int SQL_TIME        =7;
//	public final int SQL_DATE        =8;
//	public final int SQL_NUMERIC     =9;

	public final int LANG_MAL        =0;
	public final int LANG_MIL        =1;
	public final int LANG_SQL        =2;
	public final int LANG_XCORE      =3;
	public final int LANG_XQUERY     =4;


	public final String PLACEHOLDER    ="?";


	private String version;
	private String mapiversion;
	private boolean blocked= false;
	private String host = "localhost";
	private int port = 50001;
	private String username= "anonymous";
	private String password="";
	private String language= "mil";
	private int languageId= LANG_MAL;
	private String dbname= "unknown";
	
	private boolean everything = false;
	private boolean trace = false;
	private boolean connected= false;
	private boolean active= false;
	private boolean eos = false;

	// CacheBlock cache;
	IoCache blk= new IoCache();
    
	private int error = MOK;
	private String errortext="";
	private String action="";
	
	private String query;
	private String qrytemplate;
	private String prompt;
	
	// Tabular cache
	private int fieldcnt= 0;
	private int maxfields= 32;
	private int minfields= 0;
	private int rows_affected =0;
	private RowCache cache= new RowCache();
	private Column columns[]= new Column[32];
	
	private Socket socket;
	private DataOutputStream toMonet;
	//private DataInputStream fromMonet;
	private BufferedReader fromMonet;
	private DataOutputStream traceLog;
	

private void check(String action){
	if( !connected) setError("Connection lost",action);
	clrError();
}
/* 
The static method hostname gets the hostname from the monetport. The
monetport is defined by 'hostname:portnr'. The static method
portnr will return the port number in this string.
*/
private String hostname( String monetport ){
	int pos = monetport.indexOf(':');
	if (pos <= 1) return host;
	return host= monetport.substring( 0,pos);
}

private int portnr( String monetport ){
	int pos = monetport.indexOf(':');
	if (pos >= 0 && pos < monetport.length()){
		return port=Integer.parseInt(monetport.substring( pos+1 ));
	} 
	return port;
}
// To make life of front-end easier they may call for
// properties to be understood by each running server.
// The actual parameter settings may at a later stage
// be obtained from some system configuration file/ Mguardian
public String getDefaultHost(){ return "localhost";}
public int getDefaultPort(){ return 50000;}
public String getDefaultLanguage(){ return "mil";}
	
public Mapi(){}
public Mapi( String host, int port, String user, String pwd, String lang)
throws MapiException
{
	connect(host,port,user,pwd,lang);
}
	
//------------------ The actual implementation ---------
/**
 * the default communication mode is current line-based
 * a future version should also support the more efficient block mode
*/
public void setBlocking(boolean f){
	blocked= f;
}
/**
 * Toggle the debugging flag for the Mapi interface library
 * @param flg the new value 
*/
public void trace(boolean flg){
	check("trace");
	trace= flg;
}

/**
 * Open a file to keep a log of the interaction between client and server.
 * @param fnme the file name
*/
public void traceLog(String fnme){
	check("traceLog");
	System.out.println("setTraceLog not yet implemented");
}

/**
 * This method can be used to test for the lifelyness of the database server.
 * The ping query depends on the language scenario set by the user.
 * @return the boolean indicating success of failure
*/
public boolean ping(){
	//if( language.equals("mal")) query("print(1);");
	check("ping");
	return true;
}

/**
 * This method can be used to set the Mapi descriptor once an error
 * has been detected.
 * @return the MAPI internal success/failure indicator
*/
private int setError(String msg, String act){
	errortext= msg;
	error= MERROR;
	action= act;
	return MERROR;
}

/**
 * This method can be used to clear the Mapi error descriptor properties.
*/
private void clrError(){
	errortext="";
	error= MOK;
	action="";
}
/**
 * This method can be used to connect to a running server.
 * The user can indicate any of the supported query languages, e.g.
 * MIL, MAL, or SQL.
 * The method throws an exception when a basic IO error is detected.
 * @param host - a hostname, e.g. monetdb.cwi.nl
 *        port - the port to which the server listens, e.g. 50001
 *        user - user name, defaults to "anonymous"
 *        pwd  - password, defaults to none
 *        lang - language interpretation required
*/
public Mapi connect( String host, int port, String user, String pwd, String lang )
throws MapiException
{
	error = MOK;
	this.host =host;
	this.port = port;
	connected= false;
	try{
		if(trace) 
			System.out.println("setup socket "+host+":"+port);
		socket   = new Socket( host, port );
		fromMonet= new BufferedReader(
			new InputStreamReader(socket.getInputStream()));
		//fromMonet= new DataInputStream(socket.getInputStream());
		toMonet= new DataOutputStream(socket.getOutputStream());
		connected = true;
	} catch(IOException e) {
		error= MERROR;
		errortext= "Failed to establish contact\n" ;
		throw new MapiException( errortext );
	}
	blocked=false;
	this.mapiversion= "1.0";
	this.username= user;
	this.password = pwd;
	this.active = true;
	if(trace) System.out.println("sent initialization command");
	if( pwd.length()>0) pwd= ":"+pwd;
	if( blocked)
		toMonet(user+pwd+":blocked\n");
	else 	toMonet(user+pwd+"\n");
/* 
The rendezvous between client application and server requires
some care. The general approach is to expect a property table
from the server upon succesful connection. An example feedback is:
	[ "version",	"4.3.12" ]
	[ "language",	"sql" ]
	[ "dbname",	"db" ]
	[ "user",	"anonymous"]
These properties should comply with the parameters passed to connect,
in the sense that non-initialized paramaters are filled by the server.
Moreover, the user and language parameters should match the value returned.
The language property returned should match the required language interaction.
*/
	while( !gotError() && active && fetchRow()>0){
		if( cache.fldcnt[cache.reader]== 0){
			System.out.println("Unexpected reply:"
				+ cache.rows[cache.reader]);
			continue;
		}
		String property= fetchField(0);
		String value= fetchField(1);
		//System.out.println("fields:"+property+","+value);
		if( property.equals("language") && !lang.equals("") &&
		   !value.equals(lang)){
			setError("Incompatible language requirement","connect");
			System.out.println("exepcted:"+lang);
		} else language= lang;
		if( property.equals("version")) version= value;
		if( property.equals("dbname")) dbname= value;
			
	}
	if( gotError()) {
		connected = false;
		active= false;
		if(trace) System.out.println("Error occurred in initialization");
		return this;
	}
	if(trace) System.out.println("Connection established");
	connected= true;
	return this;
}

/**
 * Secure communication channels require SSL.
 * @param host - a hostname, e.g. monetdb.cwi.nl
 *        port - the port to which the server listens, e.g. 50001
 *        user - user name, defaults to "anonymous"
 *        pwd  - password, defaults to none
 *        lang - language interpretation required
*/
public static Mapi connectSSL( String host, int port, String user, String pwd, String lang )
{
	System.out.println("connectSSL not yet implemented");
	return null;
}

/**
 * Establish a secondary access structure to the same server
*/
public Mapi(Mapi m)
throws MapiException
{
	connect(m.host, m.port, m.username, m.password, m.language);
}

/**
 * The user can create multiple Mapi objects to keep pre-canned queries around.
 * @param src the Mapi connection to be duplicated
 * @return a duplicate connection
*/
public Mapi dup(Mapi j){
	check("dup");
	if( gotError()) return null;
	Mapi m= new Mapi();
	m.socket = j.socket;
	m.fromMonet = j.fromMonet;
	m.toMonet = j.toMonet;
	return m;
}

/**
 * The interaction protocol with the monet database server is very
 * simple. Each command (query) is ended with '\n'. The synchronization
 * string is recognized by brackets '\1' and '\2'.
*/
private void promptMonet() throws MapiException  {	
	// last line read is in the buffer
	int lim = blk.buf.length();
	
	prompt= blk.buf.substring(1,lim-1);
	// finding the prompt indicates end of a query
	active= false;
	if( trace) System.out.println("promptText:"+prompt);
    }

/**
 * Disconnect from Monet server.
 * The server will notice a broken pipe and terminate.
 * The intermediate structurs will be cleared.
*/
public int disconnect() {
	check("disconnect");
	if( gotError()) return MERROR;
	cache= new RowCache();
	blk= new IoCache();
	connected= active= false;
	toMonet = null; fromMonet = null; traceLog = null;
	return MOK;
}
/**
 * Reset the communication channel, thereby clearing the
 * global variable settings
 */
public int reconnect(){
	check("reconnect");
	if( gotError()) return MERROR;
	if( connected) disconnect();
	cache= new RowCache();
	blk= new IoCache();
	try{
		connect(host, port,username,password,language);
	} catch( MapiException e) {}
	return MOK;
}
/**
 * Eat away all the remaining lines
*/
private int finish_internal(){
	try{
		while(active && fetchLineInternal() != null);
	} catch( MapiException e) {}
	return MOK;
}
public int finish(){
	check("finish");
	return finish_internal();
}
//======================= Binding parameters ================

private boolean checkColumns(int fnr, String action){
	if( fnr<0){
		setError("Illegal field nummer:"+fnr,action);
		return false;
	}
	if( fnr >= fieldcnt) extendColumns(fnr);
	return true;
}
/**
 * Binding an output parameter to an object simplifies transfer,
 * values are passed to the receiver using the method setValue();
 * @param fnr a valid field index
 * obj a reference to an arbitrary receiving object
*/
public int bind(int fnr, Object o){
	check("bind");
	if( !checkColumns(fnr,"bind")) return MERROR;
	System.out.println("bind() yet supported");
	//columns[fnr].outparam = o;
	return MOK;
}
/**
 * Binding an input parameter to an object simplifies transfer,
 * values are passed to the receiver using the method setValue();
 * @param fnr a valid field index
 * obj a reference to an arbitrary receiving object
*/
public int bindVar(int fnr, int tpe, Object o){
	check("bindVar");
	if( !checkColumns(fnr,"bindVar")) return MERROR;
	System.out.println("bindVar() not supported");
	//columns[fnr].outparam = o;
	return MOK;
}

/**
 * Binding an input parameter to an object simplifies transfer,
 * values are passed to the receiver using the method setValue();
 * @param fnr a valid field index
 * scale
 * precision
 * obj a reference to an arbitrary receiving object
*/
public int bindNumeric(int fnr, int scale, int prec, Object o){
	check("bindNumeric");
	if( !checkColumns(fnr,"bindVar")) return MERROR;
	System.out.println("bindVar() not supported");
	columns[fnr].scale = scale;
	columns[fnr].precision = prec;
	//columns[fnr].outparam = o;
	return MOK;
}

// ------------------------ Columns handling
/**
 * Columns management realizes the tabular display structure envisioned.
*/
private int extendColumns(int mf){
	if(mf<maxfields) return MOK;
	int nm= maxfields+32;
	if( nm <= mf)
		nm= mf+32;
	if( trace) System.out.println("extendColumns:"+nm);
	Column nf[]= new Column[nm];
	System.arraycopy(columns,0,nf,0,maxfields);
	columns= nf;
	maxfields= nm;
	return MOK;
}

private void extendFields(int cr){
	if( trace) System.out.println("extendFields:"+cr);
	if(cache.fields== null ){
		String anew[]= new String[maxfields];
		if( cache.fields[cr]!= null)
			System.arraycopy(cache.fields[cr],0,anew,0,cache.fldcnt[cr]);
		cache.fields[cr]= anew;
	}
}

/** 
 * The values of a row are delivered to any bounded variable before
 * fetch_row returns. Automatic conversion based on common types
 * simplifies the work for the programmers.
*/
private void storeBind(){
	System.out.print("storeBind() Not supported");
	//int cr= cache.reader;
	//for(int i=0; i< fieldcnt; i++)
	//if( columns[i].outparam != null){
		//columns[i].outparam.setValue(s);
	//} 
}

/**
 * The slice row constructs a list of string field values.
 * It trims the string quotes but nothing else.
*/
public int sliceRow(){
	int cr= cache.reader;
	if(cr<0 || cr >= cache.writer){
		setError("Current row missing","sliceRow");
		return 0;
	}
	String s= cache.rows[cr];
	if( s== null){
		setError("Current row missing","sliceRow");
		return 0;
	}
	// avoid duplicate slicing
	if( cache.fldcnt[cr]>0) return cache.fldcnt[cr];
	if( s.length()==0) return 0;
	char p[]= s.toCharArray();
	int i=0;
	int f=1,l;

	if( p[0]=='!'){
		String olderr= errortext;
		clrError();
		setError(olderr+s,"sliceRow");
		return 0;
	}
	if( p[0]!='['){
		if(trace) System.out.println("Single field:"+s);
		cache.fields[cr][0]= s;
		// count filds by counting the type columns in header
		// each type looks like (str)\t
		i=0;
		for(int k=1; k<p.length; k++)
		if( p[k]=='\t' && p[k-1]==')') i++;
		if( fieldcnt<i) fieldcnt= i;
		if(trace) System.out.println("type cnt:"+i);
		return 1;
	}

	if( trace) System.out.println("slice:"+(p.length)+":"+s);
	do{
		// skip leading blancs
		while(f<p.length )
		if( p[f]=='\t' || p[f] ==' ') f++; else break; 
		if(f==p.length || p[f]==']') break;

		if(i== maxfields){
			if( extendColumns(maxfields+32) != MOK)
				return 0;
			for(int j=0;j<cache.limit;j++)
			if( cache.fields[j] != null)
				extendFields(j);
		}
		l=f+1;
		// slicing the row should respect the string/char literal
		boolean instring=s.charAt(f)=='"' || s.charAt(f)=='\'';
		char bracket= instring? s.charAt(f) :'\t';
		if(instring) f++;
		boolean done =false;
		
		while(!done && l<p.length ){
			switch(p[l]){
			case '\t':
			case ']':
				done = !instring;
				if( !done) l++;
				break;
			case ',':
				if(!instring){
					done= true;
					break;
				}
				l++;
				break;
			case '\\': l+=2; break;
			case '\'':
			case '"':
				if(instring ){
					//System.out.println("bracket:"+p[l]+l);
					if( bracket==p[l]) {
						done=true;
						break;
					}
				} 
				break;
			default: l++;
			}
		}

		//String fld= unquote(s.substring(f, l));
		String fld= s.substring(f, l);
		if(trace) System.out.println("field ["+cr+"]["
				+i+" "+l+"]"+fld+" "+instring);
		cache.fields[cr][i]= fld;
		// skip any immediate none-space
		while(l<p.length )
		if( p[l]=='\t' || p[l] ==' ') break; else l++; 
		if(trace && instring) System.out.println("skipped to:"+l);
		f= l;
		i++;
		cache.fldcnt[cr]=i;
		if(i>fieldcnt) fieldcnt=i;
	} while(f< p.length && p[f]!=']');
	if(trace) System.out.println("fields extracted:"+i+" fieldcnt:"+fieldcnt);
	return i;
}

/**
 * The fetchRow() retrieves a tuple from the server
*/
public int fetchRow(){
	check("fetchRow");
	if( getRow()==MOK ) return sliceRow();
	return 0;
}
public int fetchAllRows(){
	check("fetchAllRows");
	cacheLimit(-1);
	cache.tuples=0;
	while( getRow()== MOK)
		sliceRow();
	fetchReset();
	return cache.tuples;
}

public String fetchField(int fnr){
	check("fetchField");
	int cr= cache.reader;
	if(cr <0 || cr >cache.writer) {
		setError("No tuple in cache","fetchField"); 
		return null;
	}
	if( fnr>=0){
		if( cache.fldcnt[cr]==0){
			//System.out.println("reslice");
			sliceRow();
		}
		if( fnr < cache.fldcnt[cr])
			return cache.fields[cr][fnr];
	} 
	if( fnr== -1)
		// fetch complete tuple
		return cache.rows[cr];
	setError("Illegal field","fetchField");
	return null;
}
public String[] fetchFieldArray(int fnr){
	check("fetchField");
	int cr = cache.reader;
	if(cr <0) {
		setError("No tuple in cache","fetchFieldArray"); 
		return null;
	}

	String f[]= new String[cache.fldcnt[cr]];
	for(int i=0;i<cache.fldcnt[cr]; i++)
		f[i]= cache.fields[cr][i];
	return f;
}
public synchronized int getColumnCount(){
	check("getColumnCount");
	return fieldcnt;
}
public synchronized String getColumnName(int i){
	check("getColumnName");
	if(i<fieldcnt && columns[i]!= null && columns[i].columnname!= null)
		return columns[i].columnname;
	return "";
}
public synchronized int getRowCount(){
	check("getRowCount");
	return rows_affected;
}
public String getName(int fnr){
	check("getName");
	int cr= cache.reader;
	if(cr <0) {
		setError("No tuple in cache","getName"); 
		return null;
	}
	if( fnr >=0 && fnr < cache.fldcnt[cr]){
		if(columns[fnr].columnname== null)
			columns[fnr].columnname= "column_"+fnr;
		return columns[fnr].columnname;
	}
	setError("Illegal field","getName");
	return null;
}
public String getTable(int fnr){
	check("getTable");
	int cr= cache.reader;
	if(cr <0) {
		setError("No tuple in cache","getTable"); 
		return null;
	}
	if( fnr >=0 && fnr < cache.fldcnt[cr]){
		if(columns[fnr].tablename== null)
			columns[fnr].tablename= "table_"+fnr;
		return columns[fnr].tablename;
	}
	setError("Illegal field","getName");
	return null;
}
public int rows_affected(){
	check("rows_affected");
	return rows_affected;
}
public String getHost(){
	check("getHost");
	return host;
}
public String getUser(){
	check("getUser");
	return username;
}
public String getPassword(){
	check("getPassword");
	return password;
}
public String getLanguage(){
	check("getLanguage");
	return language;
}
public String getDBname(){
	check("getDBname");
	return language;
}
public int getPort(){
	check("getPort");
	return port;
}
public String getMapiVersion(){
	check("getMapiVersion");
	return mapiversion;
}
public String getMonetVersion(){
	check("getMonetVersion");
	return version;
}
public int getMonetId(){
	check("getMonetId");
	if(version!=null && version.charAt(0)=='4') return 4;
	return 4;
}
// ------------------------ Handling queries
/**
 * The routine mapi_check_query appends the semicolon and new line if needed.
 * Furthermore, it ensures that no 'false' newlines are sent to the server,
 * because this may lead to a synchronization error easily.
*/
private void expandQuery(String xtra){
	String n= query+xtra;
	query = n;
	if( qrytemplate != null) qrytemplate= n;
	if( trace) System.out.print("Modified query:"+query);
}

private void checkQuery(){
	clrError();
	int i = query.indexOf('\n');
	if( i>=0 && i < query.length()-1)
		setError("Newline in query string not allowed","checkQuery");
	query.replace('\n',' ');
	// trim white space
	int j= query.length()-1;
	while(j>0 && (query.charAt(j)==' ' || query.charAt(j)=='\t')) j--;
	if( j != query.length()-1) query= query.substring(0,j);
	if( language.equals("sql")){
		i= query.lastIndexOf(';');
		if( i != query.length()-1) expandQuery(";");
	}
	expandQuery("\n");
}
/**
 * The query string may contain place holders, which should be replaced
 * by the arguments presented
*/

private int prepareQueryInternal(String cmd){
	if( cmd == null || cmd.length()==0){
		// use previous query
		if(query==null) query="";
	} else {
		query = cmd;
		qrytemplate= null;
		int i= cmd.indexOf(PLACEHOLDER);
		if( i>=0 && i < cmd.length()){
			qrytemplate = query;
		}
	}
	checkQuery();
	return error;
}

/**
 * Move the query to the connection structure. Possibly interact with the
 * back-end to prepare the query for execution.
 * @param cmd the command string to be executed
*/

public int prepareQuery(String cmd){
	check("prepareQuery");
	return prepareQueryInternal(cmd);
}

/**
 * Move the query to the connection structure. Possibly interact with the
 * back-end to prepare the query for execution.
 * @param cmd the command string to be executed
 *   arg replacement strings for each of the placeholders
*/
private int prepareQueryArrayInternal(String cmd, String arg[]){
	int ret= prepareQuery(cmd);
	if( ret != MOK) return ret;

	// get rid of the previous field bindings
	for(int i=0; i< fieldcnt; i++) {
		//columns[i].inparam= null;
	}
	int lim= arg.length;
	if( lim > fieldcnt)
		extendColumns(lim);
	for(int i=0; i< lim; i++) {
		//columns[i].inparam= arg[i];
	}
	return error;
}
public int prepareQueryArray(String cmd, String arg[]){
	check("prepareQueryArray");
	return prepareQueryArrayInternal(cmd,arg);
}

/**
 * Before we ship a query, all placeholders should be removed
 * and the actual values should take their position.
 * Replacement should be able to hangle PLACEHOLDERS in the arguments.
*/
private void paramStore(){
	if(qrytemplate == null || qrytemplate.length()==0) return;
	query = qrytemplate;	

	int p, pr=0;
	for(int i=0; i< fieldcnt; i++){
		String left,right;
		p= query.indexOf(PLACEHOLDER,pr);
		if( p == pr){
			// no more placeholders
			setError("Not enough placeholders","paramStore");
			break;
		}
		left = query.substring(0,p-1);
		right= query.substring(p+1,query.length());
		//query= left+columns[i].inparam.toString()+right;
	}
	if( trace) System.out.println("paramStore:"+query);
}

/**
 * The command is shipped to the backend for execution. A single answer
 * is pre-fetched to detect any runtime error. A NULL command is
 * interpreted as taking the previous query. MOK is returned upon success.
 * The fake queries '#trace on' and '#trace off' provide an easy means
 * to toggle the trace flag.
*/
private int executeInternal(){
	paramStore();
	cacheResetInternal();
	if(trace) System.out.print("execute:"+query);
	if( query.indexOf("#trace on")==0){
		System.out.println("Set trace on");
		trace= true;
	}
	if( query.indexOf("#trace off")==0){
		System.out.println("Set trace off");
		trace= false;
	}
	try{
		toMonet(query);
	} catch( MapiException m){
		setError("Write error on stream","execute");
	}	
	return error;
}
public int execute(){
	check("execute");
	return executeInternal();
}
/**
 * The command is shipped to the backend for execution. A single answer
 * is pre-fetched to detect any runtime error. A NULL command is
 * interpreted as taking the previous query. MOK is returned upon success.
 * @param arg the list of place holder values
*/
public int executeArray(String arg[]){
	prepareQueryArrayInternal(query,arg);
	return error==MOK ? executeInternal(): error;
}

/**
 * This routine sends the Command to the database server.
 * It is one of the most common operations.
 * If Command is null it takes the last query string kept around.
 * It returns zero upon success, otherwise it indicates a failure of the request.
 * The command response is buffered for consumption, e.g. fetchRow();
 * @param cmd - the actual command to be executed

*/
private int answerLookAhead(){
	// look ahead to detect errors
	int oldrd= cache.reader;
	do{
		getRow();
	} while(error==MOK && active &&
		cache.writer+1< cache.limit);
	cache.reader= oldrd;
	if(trace ) System.out.println("query return:"+error);
	return error;
}
public int query(String cmd){
	if(cmd == null) return setError("Null query","query");
	prepareQueryInternal(cmd);
	if( error== MOK) executeInternal();
	if( error== MOK) answerLookAhead();
	return error;
}

/**
 * This routine sends the Command to the database server.
 * It is one of the most common operations.
 * If Command is null it takes the last query string kept around.
 * It returns zero upon success, otherwise it indicates a failure of the request.
 * The command response is buffered for consumption, e.g. fetchRow();
 * @param cmd - the actual command to be executed
   arg - the place holder values

*/
public int queryArray(String cmd, String arg[]){
	if(cmd == null) return setError("Null query","queryArray");
	prepareQueryArrayInternal(cmd,arg);
	if( error== MOK) executeInternal();
	if( error== MOK) answerLookAhead();
	return error;
}

/**
 * To speed up interaction with a terminal front-end,
 * the user can issue the quick_*() variants.
 * They will not analyse the result for errors or
 * header information, but simply through the output
 * received from the server to the stream indicated.
*/
public int quickQuery(String cmd, DataOutputStream fd){
	check("quickQuery");
	if(cmd == null) return setError("Null query","queryArray");
	prepareQueryInternal(cmd);
	if( error== MOK) executeInternal();
	if( error== MOK) quickResponse(fd);
	if(trace && error !=MOK) System.out.println("query returns error");
	return error;
}
public int quickQueryArray(String cmd, String arg[], DataOutputStream fd){
	check("quickQueryArray");
	if(cmd == null) return setError("Null query","quickQueryArray");
	prepareQueryArrayInternal(cmd,arg);
	if( error== MOK) executeInternal();
	if( error== MOK) quickResponse(fd);
	if(trace && error !=MOK) System.out.println("query returns error");
	return error;
}
/**
 * Stream queries are request to the database engine that produce a stream
 * of answers of indefinite length. Elements are eaten away using the normal way.
 * The stream ends with encountering the prompt. 
 * A stream query can not rely on upfront caching.
 * The stream query also ensures that the cache contains a sliding window
 * over the stream by shuffling tuples once it is filled.
 * @param cmd - the query to be executed
 *        window- the window size to be maintained in the cache
*/
public int openStream(String cmd, int windowsize){
	check("openStream");
	if(cmd == null) return setError("Null query","openStream");
	query(cmd);
	if( gotError()) return error;
	// reset the active flag to enable continual reads
	cacheResetInternal();
	cacheLimit(windowsize);
	cacheShuffle(1);
	return error;
}
/**
 * A stream can be closed by sending a request to abort the
 * further generation of tuples at the server side.
 * Note that we do not wait for any reply, because this will
 * appear at the stream-reader, a possibly different thread.
*/
public int closeStream(String cmd){
	prepareQueryInternal(cmd);
	paramStore();
	try{
		toMonet(query);
	} catch( MapiException m){
		setError("Write error on stream","execute");
	}	
	return error;
}

// -------------------- Cache Management ------------------
/**
 * Empty the cache is controlled by the shuffle percentage.
 * It is a factor between 0..100 and indicates the number of space
 * to be freed each time the cache should be emptied
 * @param percentage - amount to be freed
*/
public int cacheFreeup(int percentage){
	if( cache.writer==0 && cache.reader== -1) return MOK;
	if( percentage==100){
		//System.out.println("allocate new cache struct");
		cache= new RowCache();
		return MOK;
	}
	if( percentage <0 || percentage>100) percentage=100;
	int k= (cache.writer * percentage) /100;
	if( k < 1) k =1;
	System.out.println("shuffle cache:"+percentage+" tuples:"+k);
	for(int i=0; i< cache.writer-k; i++){
		cache.rows[i]= cache.rows[i+k];
		cache.fldcnt[i]= cache.fldcnt[i+k];
		cache.fields[i]= cache.fields[i+k];
	}
	for(int i=k; i< cache.limit; i++){
		cache.rows[i]= null;
		cache.fldcnt[i]=0;
		cache.fields[i]= new String[maxfields];
		for(int j=0;j<maxfields;j++)
			cache.fields[i][j]= null;
	}
	cache.reader -=k;
	if(cache.reader < -1) cache.reader= -1;
	cache.writer -=k;
	if(cache.writer < 0) cache.writer= 0;
	System.out.println("new reader:"+cache.reader+" writer:"+cache.writer);
	return MOK;
}
/**
 * CacheReset throws away any tuples left in the cache and
 * prepares it for the next sequence;
 * It should re-size the cache too.
 * It should retain the field information
*/
private void cacheResetInternal()
{
	finish_internal();
	rows_affected= 0;
	active= true;
	if( cache.fldcnt==null)
		cache.fldcnt = new int[cache.limit];
	fieldcnt=0;
	cache.tuples= 0;
	cacheFreeup(100);
}

/**
 * Users may change the cache size limit
 * @param limit - new limit to be obeyed
 *	shuffle - percentage of tuples to be shuffled upon full cache.
*/
public int cacheLimit(int limit){
	cache.rowlimit= limit;
	return MOK;
}
/**
 * Users may change the cache shuffle percentage
 * @param shuffle - percentage of tuples to be shuffled upon full cache.
*/
public int cacheShuffle(int shuffle){
	if( shuffle< -1 || shuffle>100) {
		cache.shuffle=100;
		return setError("Illegal shuffle percentage","cacheLimit");
	}
	cache.shuffle= shuffle;
	return MOK;
}

/**
 * Reset the row pointer to the first line in the cache.
 * This need not be a tuple.
 * This is mostly used in combination with fetching all tuples at once.
 */
public int fetchReset(){
	check("fetchReset");
	cache.reader = -1;
	return MOK;
}

/**
 * Reset the row pointer to the requested row number;
 * Comments lines are ignored.
 * @param rownr - the row of interest
 */
public int seekRow(int rownr){
	check("seekRow");
	int i, sr=rownr;
	cache.reader= -1;
	if( rownr<0) return setError("Illegal row number","seekRow");
	for(i=0; rownr>=0 && i<cache.writer; i++){
		if( cache.rows[i].indexOf("#")==0 ) continue;
		if( cache.rows[i].indexOf("!")==0 ) continue;
		if( --rownr <0) break;
	}
	if( rownr>=0) return setError("Row not found "+sr+" tuples "+cache.tuples,"seekRow");
	cache.reader= i;
	return MOK;
}
/**
 * These are the lowest level operations to retrieve a single line from the 
 * server. If something goes wrong the application may try to skip input to 
 * the next synchronization point.
 * If the cache is full we reshuffle some tuples to make room.
*/
private void extendCache(){
	int oldsize= cache.limit;
	if( oldsize == cache.rowlimit){
		System.out.println("Row cache limit reached extendCache");
		setError("Row cache limit reached","extendCache");
		// shuffle the cache content
		if( cache.shuffle>0)
		System.out.println("Reshuffle the cache ");
	}
	int incr = oldsize /10;
	int newsize = oldsize +(incr<100? 100: incr);
	if( newsize >cache.rowlimit && cache.rowlimit>0)
		newsize = cache.rowlimit;

	String newrows[]= new String[newsize];
	if(oldsize>0){
		System.arraycopy(cache.rows,0,newrows,0,oldsize);
		cache.rows= newrows;
		//if(trace) System.out.println("Extend the cache.rows storage");
	}
	    
	int newfldcnt[]= new int[newsize];
	if(oldsize>0){
		System.arraycopy(cache.fldcnt,0,newfldcnt,0,oldsize);
		cache.fldcnt= newfldcnt;
		//if(trace) System.out.println("Extend the cache.fldcnt storage");
		for(int i=oldsize;i<newsize;i++) cache.fldcnt[i]=0;
	}
	String newfields[][]= new String[newsize][];
	if(oldsize>0){
		System.arraycopy(cache.fields,0,newfields,0,oldsize);
		cache.fields= newfields;
		//if(trace) System.out.println("Extend the cache.fields storage");
		for(int i=oldsize;i<newsize;i++) 
			cache.fields[i]= new String[maxfields];
	}
	cache.limit= newsize;
}

private int clearCache(){
	// remove all left-over fields
	if( cache.reader+2<cache.writer){
		System.out.println("Cache reset with non-read lines");
		System.out.println("reader:"+cache.reader+" writer:"+cache.writer);
		setError("Cache reset with non-read lines","clearCache");
	}
	cacheFreeup(cache.shuffle);
	//cacheResetInternal();
	return MOK;
}
    
// ----------------------- Basic line management ----------------
/**
 * The fetchLine() method is the lowest level of interaction with
 * the server to acquire results. The lines obtained are stored in 
 * a cache for further analysis. 
*/
public String fetchLine() throws MapiException {
	check("fetchLine");
	return fetchLineInternal();
}
public String fetchLineInternal() throws MapiException {
	if( cache.writer>0 && cache.reader+1<cache.writer){
		if( trace) System.out.println("useCachedLine:"+cache.rows[cache.reader+1]);
		return cache.rows[++cache.reader];
	}
	if( ! active) return null;
	error= MOK;
	// get rid of the old buffer space
	if( cache.writer ==cache.rowlimit){
		 clearCache();
	}

	// check if you need to read more blocks to read a line
	blk.eos= false;

	// fetch one more block or line
	int n=0;
	int len= 0;
	String s="";
	blk.buf= null;
	if( !connected){
		setError("Lost connection with server","fetchLine");
		return null;
	}
	if( trace) System.out.println("Start reading from server");
	try {
		blk.buf = fromMonet.readLine();
		if(trace) System.out.println("gotLine:"+blk.buf);
	} catch(IOException e){
		connected= false;
		error= Mapi.MAPIERROR;
		errortext= "Communication broken";
		throw new MapiException( errortext);
	}
	if( blk.buf==null ){
		blk.eos= true;	
		setError("Lost connection with server","fetchLine");
		connected= false;
		return null;
	}
	if( blk.buf.length()>0){
		switch( blk.buf.charAt(0)){
		case PROMPTBEG:
			promptMonet();
			return null;
		case '[': cache.tuples++;
		}
	}	    

	// we have at least one complete line in the buffer
        // manage the row cache space first
        if( cache.writer >= cache.limit)
		extendCache();
        cache.rows[cache.writer] = blk.buf;
        cache.writer++;
        return cache.rows[++cache.reader];
}

/**
 * If the answer to a query should be simply passed on towards a client,
 * i.e. a stream, it pays to use the quickResponse() routine.
 * The stream is only checked for occurrence of an error indicator
 * and the prompt.
 * The best way to use this shortcut execution is calling
 * mapi_quick_query(), otherwise we are forced to first
 * empy the row cache.
 * @param fd - the file destination
*/

public int quickResponse(DataOutputStream fd){
	String msg;

	if( fd== null)
		return setError("File destination missing","response");
	try{
		while( active && (msg=fetchLineInternal()) != null)
		try{
			fd.writeBytes(msg.toString());
			fd.writeBytes("\n");
			fd.flush();
		} catch( IOException e){
			throw new MapiException("Can not write to destination" );
		}
	} catch(MapiException e){
	}
	return error;

}

//------------------- Response analysis --------------
private void keepProp(String name, String colname){
}

// analyse the header row, but make sure it is restored in the end.
// Note that headers received contains an addition column with
// type information. It should be ignored while reading the tuples.
private void headerDecoder(){
	String line= cache.rows[cache.reader];
	if( trace) System.out.println("header:"+line);
	int etag= line.lastIndexOf("#");
	if( etag==0 || etag== line.length()) return;
	String tag= line.substring(etag);


	cache.rows[cache.reader]="[ "+cache.rows[cache.reader].substring(1,etag);
	int cnt= sliceRow();
	if(trace) System.out.println("columns "+cnt);
	extendColumns(cnt);
	if( tag.equals("# name") ){
		for(int i=0;i<cnt;i++){
			if(columns[i]==null) columns[i]= new Column();
			String s= columns[i].columnname= fetchField(i);
		}
	} else	
	if( tag.equals("# type") ){
		for(int i=0;i<cnt;i++){
			if(columns[i]==null) columns[i]= new Column();
			String s= columns[i].columntype= fetchField(i);
		}
	} else	
	if( trace) System.out.println("Unrecognized header tag "+tag);
	//REST LATER

	cache.rows[cache.reader]= line;
}

// Extract the next row from the cache.
private int getRow(){
        String reply= "";

	//if( trace) System.out.println("getRow:active:"+active+
		//" reader:"+(cache.reader+1)+" writer:"+cache.writer);
        while( active ||cache.reader+1< cache.writer){
		if( active){
			try{  
				reply= fetchLineInternal();
			} catch(MapiException e){ 
				if(trace)
				 System.out.print("Got exception in getRow");
				reply=null;
			}
			if( gotError() || reply == null) return MERROR;
		} else reply = cache.rows[++cache.reader];

		if(trace) System.out.println("getRow:"+cache.reader);
		if( reply.length()>0)
		switch(reply.charAt(0)){
		    case '#': 
			headerDecoder();
			return getRow();
		    case '!': 
			// concatenate error messages
			String olderr= errortext;
			clrError();
			setError(olderr+reply.toString(),"getRow");
			return getRow();
		    default:
			return MOK;
		}
        }
        return MERROR;
}
// ------------------------------- Utilities
/** The java context provides a table abstraction 
 * the methods below provide access to the relevant Mapi components
*/
public int getTupleCount(){
	int cnt=0;
	for(int i=0;i<cache.writer;i++){
		if(cache.rows[i].charAt(0)=='[') cnt++;
	}
	//System.out.println("counted "+cnt+" maintained "+cache.tuples);
	return cnt;
}
/**
 * unquoteInternal() squeezes a character array to expand
 * all quoted characters. It also removes leading and trailing blancs.
 * @param msg - the byte array to be 
*/
private int unquoteInternal(char msg[], int first)
{
	int f=0, l=0;
	for(; f< msg.length; f++)
	if( msg[f] != ' ' && msg[f]!= '\t') break;

	if( f< msg.length && (msg[f]=='"' || msg[f]=='\'')){
		f++;
		for(l=f+1; l<msg.length && !(msg[l]=='"' || msg[l]=='\''); l++);
		l--;
	} else {
	}
	return f;
}

public String quote(String msg){
	System.out.println("Not yet implemented");
	return null;
}
public String unquote(String msg){
	char p[]= msg.toCharArray();
	int f=0,l=f;
	char bracket= ' ';
	while(f<p.length && p[f]==' ') f++;
	if( f<p.length) bracket = p[f];
	if( bracket == '"' || bracket== '\''){
		l= msg.lastIndexOf(bracket);
		if( l<=f){
			if(trace) 
			System.out.println("closing '"+bracket+"' not found:"+msg);
			return msg;
		}
		return msg.substring(f+1,l);
	} else 
	// get rid of comma except when it is a literal char
	if( p[f]!=',' && p[p.length-1]==',') msg= msg.substring(f,p.length-1);
	return msg; 
}

public boolean gotError(){
	return error!=MOK;
}
public String getErrorMsg(){
	return errortext;
}
public String getExplanation(){
    return "Mapi:"+dbname+"\nERROR:"+errortext+"\nERRNR:"+error+
	   "\nACTION:"+action+"\nQUERY:"+query+"\n";
}

public String getPrompt(){
	return prompt;
}
public boolean isConnected(){
    return connected;
}


/* 
The low-level operation toMonet attempts to sent a string to the server.
It assumes that the string is properly terminated and a connection to
the server still exists.
*/
private void toMonet(String msg) throws MapiException {
	if( ! connected)
		throw new MapiException( "Connection was disabled" );

	if( msg== null || msg.equals("")){
		if(trace)
			System.out.println("Attempt to send an empty message");
		return;
	}
	try{
		if( trace) System.out.println("toMonet:"+msg);
		int size= msg.length();
		toMonet.writeBytes(msg);
		toMonet.flush();
	} catch( IOException e){
		throw new MapiException("Can not write to Monet" );
	}
}

/**
 * The routine timeout can be used to limit the server handling requests.
 * @parem time the timeout in milliseconds
*/
public int timeout(int time){
	check("timeout");
	System.out.println("timeout() not yet implemented");
	return MOK;
}

/**
 * The routine explain() dumps the status of the latest query to standard out.
*/
public int explain(){
	System.out.println(getExplanation());
	return MOK;
}

class IoCache
{   String  buf="";
    boolean eos;
    public IoCache(){
        eos = false;
    }
}
class RowCache{
    int rowlimit= -1;   /* maximal number of rows to cache */
    int shuffle= 100;	/* percentage of tuples to shuffle upon overflow */
    int limit=100;      /* current storage space limit */
    int writer= 0;
    int reader= -1;
    int tuples=0;     /* actual tuples in cache */
    int  fldcnt[] = new int[limit];   /* actual number of columns in each row */
    String rows[]= new String[limit]; /* string representation of rows received */
    String fields[][]= new String[limit][];
    public RowCache(){
	for(int i=0;i<limit; i++){
		fields[i]= new String[maxfields];
		rows[i]=null;
		fldcnt[i]=0;
		for(int j=0;j<maxfields;j++) fields[i][j]= null;
	}
    }
}
class Column{
    String tablename=null;
    String columnname=null;
    String columntype=null;
    int  colwidth;
    int  coltype;
    int  precision;
    int  scale;
    int  isnull;

    //Object inparam;	// to application variable 
    //Object outparam;	// both are converted to strings
}
}

