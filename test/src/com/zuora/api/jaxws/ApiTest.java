package com.zuora.api.jaxws;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import com.zuora.api.InvalidTypeFault;
import com.zuora.api.LoginFault;
import com.zuora.api.LoginResult;
import com.zuora.api.Error;

import com.zuora.api.QueryResult;
import com.zuora.api.RatePlanData;
import com.zuora.api.SaveResult;
import com.zuora.api.SessionHeader;
import com.zuora.api.Soap;

import com.zuora.api.SubscribeOptions;
import com.zuora.api.SubscribeRequest;
import com.zuora.api.SubscribeResult;
import com.zuora.api.SubscriptionData;
import com.zuora.api.UnexpectedErrorFault;

import com.zuora.api.ZuoraService;
import com.zuora.api.object.Account;
import com.zuora.api.object.Amendment;
import com.zuora.api.object.Contact;
import com.zuora.api.object.InvoicePayment;
import com.zuora.api.object.Payment;
import com.zuora.api.object.PaymentMethod;
import com.zuora.api.object.Product;
import com.zuora.api.object.ProductRatePlan;
import com.zuora.api.object.RatePlan;
import com.zuora.api.object.Subscription;
import com.zuora.api.object.Usage;
import com.zuora.api.object.ZObject;

public class ApiTest {
	
	private static final String PROPERTY_PRODUCT_NAME = "productName";
	private static final String FILE_PROPERTY_NAME = "test.properties";
	private static final String PROPERTY_USERNAME = "username";
	private static final String PROPERTY_PASSWORD = "password";
	
	private SessionHeader header;
	private ZuoraService service;
	private Soap soap;
	private Properties properties;
	public ApiTest()  {
	        this.service=new ZuoraService();
	        this.soap = service.getSoap();
	}

	public static void main(String[] arg) {

		try {
			ApiTest test = new ApiTest();
			test.login();
			if("all".equals(arg[0])){
	        	 test.testCreateAccount();
	        	 System.out.println("");
	        	 test.testSubscribe();
	        	 System.out.println("");
	        	 test.testSubscribeWithNoPayment();
	        	 System.out.println("");
	        	 test.testUpgradeAndDowngrade();
	        	 System.out.println("");
	        	 test.testSubscribeWithExistingAccount();
	        	 System.out.println("");
	        	 test.testCancelSubscription();
	        	 System.out.println("");
	        	 test.testCreatePayment();
	        	 System.out.println("");
	        	 test.testAddUsage();
	         }
	         else if("c-account".equals(arg[0])){
	        	 test.testCreateAccount();
	         }
	         else if("c-subscribe".equals(arg[0])){
	        	test.testSubscribe(); 
	         }
	         else if("c-subscribe-no-p".equals(arg[0])){
	        	 test.testSubscribeWithNoPayment(); 
	         }
	         else if("c-subscribe-w-existingAccount".equals(arg[0])){
	        	 test.testSubscribeWithExistingAccount(); 
	         }
	         else if("c-subscribe-w-amendment".equals(arg[0])){
	        	 test.testUpgradeAndDowngrade();
	         }
	         else if("cnl-subscription".equals(arg[0])){
	        	 test.testCancelSubscription();
	         }
	         else if("c-payment".equals(arg[0])){
	        	 test.testCreatePayment();
	         }
	         else if("c-usage".equals(arg[0])){
	        	 test.testAddUsage();
	         }
	         else if("help".equals(arg[0])){
	        	 printHelp();
	         }
	         else{
	        	 System.out.println("command not found.");
	        	 printHelp();
	         }
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

	private void login() throws Exception {
		LoginResult lr = soap.login(getPropertyValue(PROPERTY_USERNAME), getPropertyValue(PROPERTY_PASSWORD));
		this.header=new SessionHeader();
		this.header.setSession(lr.getSession());
	}
	 private void testCreateAccount()throws Exception{
		   System.out.println("Account Create....");
		   String accountId=createAccount(true);
		   System.out.println("Account Created:"+accountId);
	   }
	 private void testSubscribe() throws Exception {
		   System.out.println("Subscribe call....");
		   List<SubscribeResult> result=createSubscribe(Boolean.TRUE);
		   System.out.println(createMessage(result));
		   Subscription sQuery = querySubscription(result.get(0).getSubscriptionId().getValue());
	       System.out.println("Subscription created:"+ sQuery.getName().getValue());
	   }
	 private void testSubscribeWithNoPayment()throws Exception{
		   System.out.println("Subscribe(no payments) call....");
		   List<SubscribeResult> result = createSubscribe(Boolean.FALSE);
		   System.out.println(createMessage(result));
		   Subscription sQuery = querySubscription(result.get(0).getSubscriptionId().getValue());
	       System.out.println("Subscription created:"+ sQuery.getName().getValue());
	   }
	 public String createAccount(boolean active) throws Exception {

	      // create account
	      Account acc1 = createAccount();
	      String accountId = create(acc1);
	      
	      if (active) {

	          // create contact
	          Contact con = createContact();
	          con.setAccountId(Custom_JAXBElement("accountId",accountId));
	          String contactId = create(con);

	          PaymentMethod pm = createPaymentMethod();
	          pm.setAccountId(Custom_JAXBElement("accountId",accountId));
	          String pmId = create(pm);

	          // set required active fields and activate
	          Account accUpdate = new Account();
	          accUpdate.setId(Custom_JAXBElement("Id",accountId));
	          accUpdate.setStatus(Custom_JAXBElement("status","Active"));
	          accUpdate.setSoldToId(Custom_JAXBElement("soldToId",contactId));
	          accUpdate.setBillToId(Custom_JAXBElement("billToId",contactId));
	          accUpdate.setAutoPay(new JAXBElement<Boolean>(new QName("http://object.api.zuora.com/","autoPay"),Boolean.class,true));
	          accUpdate.setPaymentTerm(Custom_JAXBElement("status","Due Upon Receipt"));
	          accUpdate.setDefaultPaymentMethodId(Custom_JAXBElement("defaultPaymentMethodId",pmId));
	          update(accUpdate);
	      }

	      return accountId;
	  }

	 public Account  createAccount() throws LoginFault, UnexpectedErrorFault, InvalidTypeFault {
			// create account
		    long time = System.currentTimeMillis();       
			Account acc = new Account();
			acc.setAccountNumber(Custom_JAXBElement("accountNumber","t-" + time));
			acc.setBatch(Custom_JAXBElement("batch","Batch1"));
			acc.setBillCycleDay(10);
			acc.setAllowInvoiceEdit(new JAXBElement<Boolean>(new QName("http://object.api.zuora.com/","allowInvoiceEdit"),Boolean.class,true));
			acc.setAutoPay(new JAXBElement<Boolean>(new QName("http://object.api.zuora.com/","autoPay"),Boolean.class,false));
			acc.setCrmId(Custom_JAXBElement("crmId","SFDC-" + time));
			acc.setCurrency(Custom_JAXBElement("currency","USD"));
			acc.setCustomerServiceRepName(Custom_JAXBElement("customerServiceRepName","CSR Dude"));
			acc.setName(Custom_JAXBElement("name","tiger" + time));
//			acc.setName(new JAXBElement<String>(new QName("http://object.api.zuora.com/","name"),String.class,"tst"));
			acc.setPurchaseOrderNumber(Custom_JAXBElement("purchaseOrderNumber","PO-" + time));
		    acc.setSalesRepName(Custom_JAXBElement("salesRepName","Sales Dude"));
		    acc.setPaymentTerm(Custom_JAXBElement("PaymentTerm","Due Upon Receipt"));
			acc.setStatus(Custom_JAXBElement("status","Draft"));
			
			return acc;
			
						
		}
	 public static JAXBElement<String> Custom_JAXBElement(String n,String s) {
			QName qn=new QName("http://object.api.zuora.com/",n);
			JAXBElement<String> Jbe=new JAXBElement<String>(qn,String.class,s);
			return Jbe;
		}
	
	
	 public Contact createContact() {
	      long time = System.currentTimeMillis();
	      Contact con = new Contact();
	      con.setFirstName(Custom_JAXBElement("firstName","Firstly" + time));
	      con.setLastName(Custom_JAXBElement("lastName","secondly" + time));
	      con.setAddress1(Custom_JAXBElement("address1","52 Vexford Lane"));
	      con.setCity(Custom_JAXBElement("city","Anaheim"));
	      con.setState(Custom_JAXBElement("state","California"));
	      con.setCountry(Custom_JAXBElement("country","United States"));
	      con.setPostalCode(Custom_JAXBElement("postalCode","92808"));
	      con.setWorkEmail(Custom_JAXBElement("workEmail","contact@test.com"));
	      con.setWorkPhone(Custom_JAXBElement("workPhone","4152225151"));
	     
	      return con;
	  }
	 private PaymentMethod createPaymentMethod() {
	      PaymentMethod pm = new PaymentMethod();
	      
	      pm.setType(Custom_JAXBElement("type","CreditCard"));
	      pm.setCreditCardType(Custom_JAXBElement("creditCardType","Visa"));
	      pm.setCreditCardAddress1(Custom_JAXBElement("creaditCardAddress1","52 Vexford Lane"));
	      pm.setCreditCardCity(Custom_JAXBElement("creaditCardCity","Anaheim"));
	      pm.setCreditCardState(Custom_JAXBElement("creaditCardState","California"));
	      pm.setCreditCardPostalCode(Custom_JAXBElement("creaditCardCode","92808"));
	      pm.setCreditCardCountry(Custom_JAXBElement("creaditCardCountry","United States"));
	      pm.setCreditCardHolderName(Custom_JAXBElement("CreditCardHolderName","Firstly Lastly"));
	      
	      pm.setCreditCardExpirationYear(new JAXBElement<Integer>(new QName("http://object.api.zuora.com/","creditCardExpirationYear"),Integer.class,Calendar.getInstance().get(Calendar.YEAR)+1));
	      pm.setCreditCardExpirationMonth(new JAXBElement<Integer>(new QName("http://object.api.zuora.com/","creditCardExpirationMonth"),Integer.class,12));
	      pm.setCreditCardNumber(Custom_JAXBElement("CreditCardNumber","4111111111111111"));
	      
	      return pm;
	   }

	 private  String create(ZObject acc) throws InvalidTypeFault, UnexpectedErrorFault   {
		
			List<ZObject> list=new ArrayList<ZObject>();
			list.add(acc);
			List<SaveResult> result = soap.create(list, header);
			if(!result.get(0).isSuccess())
				System.out.println(result.get(0).getErrors().get(0).getMessage().getValue());
			String id=result.get(0).getId().getValue();
			
			
			return id;


	      
	   }
	 
	 private List<SubscribeResult> createSubscribe(boolean isProcessPayment)throws Exception{
		  ProductRatePlan prp = getProductRatePlanByProductName(getPropertyValue(PROPERTY_PRODUCT_NAME));

		  Account acc = createAccount();
	      Contact con = createContact();
	      PaymentMethod pm = createPaymentMethod();
	      Subscription subscription = createSubscription();

	      SubscribeOptions sp = new SubscribeOptions();
	      sp.setGenerateInvoice(new JAXBElement<Boolean>(new QName("http://api.zuora.com/","GenerateInvoice"),Boolean.class,true));
	      sp.setProcessPayments(new JAXBElement<Boolean>(new QName("http://api.zuora.com/","ProcessPayments"),Boolean.class,isProcessPayment));
	      
	      SubscriptionData sd = new SubscriptionData();
	      sd.setSubscription(new JAXBElement<Subscription>(new QName("http://api.zuora.com/","Subscription"),Subscription.class,subscription));
	      List<RatePlanData> subscriptionRatePlanDataArray = createRatePlanData(prp);
	      sd.getRatePlanData().addAll(subscriptionRatePlanDataArray);
	      
	      SubscribeRequest sub = new SubscribeRequest();
	      sub.setAccount(new JAXBElement<Account>(new QName("http://api.zuora.com/","Account"),Account.class,acc));
	      sub.setBillToContact(new JAXBElement<Contact>(new QName("http://api.zuora.com/","BillToContact"),Contact.class,con));
	      sub.setPaymentMethod(new JAXBElement<PaymentMethod>(new QName("http://api.zuora.com/","PaymentMethod"),PaymentMethod.class,pm));
	      sub.setSubscriptionData(new JAXBElement<SubscriptionData>(new QName("http://api.zuora.com/","SubscriptionData"),SubscriptionData.class,sd));
	      sub.setSubscribeOptions(new JAXBElement<SubscribeOptions>(new QName("http://api.zuora.com/","SubscribeOptions"),SubscribeOptions.class,sp));
	      
	      List<SubscribeRequest> subscribes=new ArrayList<SubscribeRequest>();
	      subscribes.add(sub);
	      
          List<SubscribeResult> resp=soap.subscribe(subscribes, header);
          
	      return resp;
	      
	   }
	 public String getPropertyValue(String propertyName) {
			return getProperties().getProperty(propertyName);
		}
	 public Properties getProperties() {
			if (properties == null) {
				loadProperties();
			}
			return properties;
		}
	 public void loadProperties() {
			//String subscribeDataFileName = System.getProperty(FILE_PROPERTY_NAME);
			String subscribeDataFileName=FILE_PROPERTY_NAME;
			try {
				properties = new Properties();
				if (subscribeDataFileName != null) {
					properties.load(new FileInputStream(subscribeDataFileName));
				}
			} catch (IOException e) {
				System.out.println("Error while reading input data file: " + subscribeDataFileName);
				System.out.println(e.getMessage());
			}
		}
	 private ProductRatePlan getProductRatePlanByProductName(String productName) throws Exception {
		  QueryResult result=soap.query("select Id from Product where Name = '"+productName+"'", header); 
		  List<ZObject> list = result.getRecords();
	      if(result == null || result.getSize() == 0){
	    	  System.out.println("No Product found with Name '"+productName+"'");
	      }
	      Product p = (Product)list.get(0);
		  result=soap.query("select Id,Name from ProductRatePlan where ProductId = '"+p.getId().getValue()+"'", header); 
		  list=result.getRecords();
		  ProductRatePlan rp = (ProductRatePlan)list.get(0);
	      return rp;

	   }
	 private Subscription createSubscription() throws DatatypeConfigurationException {

	      Calendar cal= Calendar.getInstance();
//		  XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
//		  XMLGregorianCalendar calendar = new XMLGregorianCalendarImpl(); 
		  XMLGregorianCalendar calendar=convert(cal);
	      Subscription sub = new Subscription();
	      
	      sub.setName(Custom_JAXBElement("name","SomeSubscription" + System.currentTimeMillis()));
	      sub.setTermStartDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","TermStartDate"),XMLGregorianCalendar.class,calendar));
	      sub.setContractEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ContractEffectiveDate"),XMLGregorianCalendar.class,calendar));
	      sub.setContractAcceptanceDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ContractAcceptanceDate"),XMLGregorianCalendar.class,calendar));
	      sub.setServiceActivationDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ServiceActivationDate"),XMLGregorianCalendar.class,calendar));
	      sub.setInitialTerm(new JAXBElement<Integer>(new QName("http://object.api.zuora.com/","InitialTerm"),Integer.class,12));
	      sub.setRenewalTerm(new JAXBElement<Integer>(new QName("http://object.api.zuora.com/","RenewalTerm"),Integer.class,12));
	      sub.setNotes(Custom_JAXBElement("notes","This is a test subscription"));

	      return sub;
	   }
	 private List<RatePlanData> createRatePlanData(ProductRatePlan ProductRatePlan) {

	      RatePlanData ratePlanData = new RatePlanData();

	      RatePlan ratePlan = new RatePlan();
	      ratePlan.setAmendmentType(Custom_JAXBElement("AmendmentType","NewProduct"));
	      ratePlan.setProductRatePlanId(Custom_JAXBElement("ProductRatePlanId",ProductRatePlan.getId().getValue()));
	      ratePlanData.setRatePlan(new JAXBElement<RatePlan>(new QName("http://api.zuora.com/","RatePlan"),RatePlan.class,ratePlan));
	      List<RatePlanData> rpd=new ArrayList<RatePlanData>();
	      rpd.add(ratePlanData);
	      return rpd;
	   }
	 private Subscription querySubscription(String subscriptionId) throws Exception {
		  QueryResult result =soap.query("SELECT id, name,status,version FROM Subscription WHERE Id = '"+subscriptionId+"'", header);
	       
	      List<ZObject> list = result.getRecords();
	      Subscription rec=(Subscription)list.get(0);
	      return rec;
	   }
	 private String update(ZObject acc) throws Exception {
		 
		    List<ZObject> list=new ArrayList<ZObject>();
			list.add(acc);
			List<SaveResult> results = soap.update(list, header);
			SaveResult result=results.get(0);
			String id=result.getId().getValue();
			return id;
			
	   }

	public XMLGregorianCalendar convert(Calendar calendar) {
		XMLGregorianCalendar cal = new XMLGregorianCalendarImpl();
		cal.setYear(calendar.get(Calendar.YEAR));
		cal.setMonth(calendar.get(Calendar.MONTH) + 1);
		cal.setDay(calendar.get(Calendar.DAY_OF_MONTH));
		cal.setHour(calendar.get(Calendar.HOUR_OF_DAY));
		cal.setMinute(calendar.get(Calendar.MINUTE));
		cal.setSecond(calendar.get(Calendar.SECOND));
		cal.setMillisecond(calendar.get(Calendar.MILLISECOND));
		cal.setTimezone(calendar.get(Calendar.ZONE_OFFSET) / 60000);
		return cal;
	}
	 private String createMessage(List<SubscribeResult> resultArray) {
	      StringBuilder resultString = new StringBuilder("SusbscribeResult :\n");
	      if (resultArray != null) {
	         SubscribeResult result = resultArray.get(0);
	         if (result.isSuccess()) {
	            resultString.append("\n\tAccount Id: ").append(result.getAccountId().getValue())
	            .append("\n\tAccount Number: ").append(result.getAccountNumber().getValue())
	            .append("\n\tSubscription Id: ").append(result.getSubscriptionId().getValue())
	            .append("\n\tSubscription Number: ").append(result.getSubscriptionNumber().getValue())
	            .append("\n\tInvoice Number: ").append(result.getInvoiceNumber().getValue());
//	            .append("\n\tPayment Transaction: ").append(result.getPaymentTransactionNumber().getValue());
	         } else {
	            resultString.append("\nSubscribe Failure Result: \n");
	            List<Error> errors = result.getErrors();
	            if (errors != null) {
	               for (Error error : errors) {
	                  resultString.append("\n\tError Code: ").append(error.getCode().toString())
	                            .append("\n\tError Message: ").append(error.getMessage());                   
	               }
	            }
	         }
	      }
	      return resultString.toString();
	   }
	 private void testSubscribeWithExistingAccount()throws Exception{
		   String accountId = createAccount(true);
		   System.out.println("Subscribe(with existing account["+accountId+"]) call....");
		   List<SubscribeResult> result=createSubscribeWithExistingAccount(accountId);
		   System.out.println(createMessage(result));
		   Subscription sQuery = querySubscription(result.get(0).getSubscriptionId().getValue());
		   System.out.println("Subscription created:"+ sQuery.getName().getValue());
	   }
	 private List<SubscribeResult> createSubscribeWithExistingAccount(String accountId)throws Exception{
		  ProductRatePlan prp = getProductRatePlanByProductName(getPropertyValue(PROPERTY_PRODUCT_NAME));
		  Subscription subscription = createSubscription();

	      SubscribeOptions sp = new SubscribeOptions();
	      sp.setGenerateInvoice(new JAXBElement<Boolean>(new QName("http://api.zuora.com/","GenerateInvoice"),Boolean.class,true));
	      sp.setProcessPayments(new JAXBElement<Boolean>(new QName("http://api.zuora.com/","ProcessPayments"),Boolean.class,true));
	      
	      SubscriptionData sd = new SubscriptionData();
	      sd.setSubscription(new JAXBElement<Subscription>(new QName("http://api.zuora.com/","Subscription"),Subscription.class,subscription));
	      List<RatePlanData> subscriptionRatePlanDataArray = createRatePlanData(prp);
	      sd.getRatePlanData().addAll(subscriptionRatePlanDataArray);
	      
	      SubscribeRequest sub = new SubscribeRequest();
	      sub.setAccount(new JAXBElement<Account>(new QName("http://api.zuora.com/","Account"),Account.class,queryAccount(accountId)));
	      sub.setSubscriptionData(new JAXBElement<SubscriptionData>(new QName("http://api.zuora.com/","SubscriptionData"),SubscriptionData.class,sd));
	      sub.setSubscribeOptions(new JAXBElement<SubscribeOptions>(new QName("http://api.zuora.com/","SubscribeOptions"),SubscribeOptions.class,sp));
	      
	      List<SubscribeRequest> subscribes=new ArrayList<SubscribeRequest>();
	      subscribes.add(sub);
	      
          List<SubscribeResult> resp=soap.subscribe(subscribes, header);
          
	      return resp;
	      
	   }
	 private Account queryAccount(String accId) throws Exception {
		  QueryResult result =soap.query("SELECT id, name, accountnumber,DefaultPaymentMethodId FROM account WHERE id = '"+accId+"'", header);
	      List<ZObject> list = result.getRecords();
	      Account rec=(Account)list.get(0);
	      return rec;
	   }
	 private void testCancelSubscription()throws Exception{
		   System.out.println("Cancel Subscribe....");
		   List <SubscribeResult> result = createSubscribe(Boolean.TRUE);
		   System.out.println(createMessage(result));
		   Subscription sub = querySubscription(result.get(0).getSubscriptionId().getValue());
		   System.out.println("Subscription created:"+ sub.getName().getValue());
		   System.out.println("Subscrption status :"+sub.getStatus().getValue());
	       
		   Calendar efd = Calendar.getInstance();
		   efd.add(Calendar.DAY_OF_MONTH, 1);
		   XMLGregorianCalendar effectiveDate=convert(efd);
		   Amendment amd = new Amendment();
		   amd.setName(Custom_JAXBElement("name","Amendment:Cancellation"));
		   amd.setEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","EffectiveDate"),XMLGregorianCalendar.class,effectiveDate));
		   amd.setType(Custom_JAXBElement("type","Cancellation"));
		   amd.setSubscriptionId(Custom_JAXBElement("subscriptionId",sub.getId().getValue()));
		   amd.setStatus(Custom_JAXBElement("status","Draft"));
		   String amdID= create(amd);
		   
		   Amendment updateAmd = new Amendment();
		   updateAmd.setId(Custom_JAXBElement("Id",amdID));
		   updateAmd.setContractEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","EffectiveDate"),XMLGregorianCalendar.class,effectiveDate));
		   updateAmd.setStatus(Custom_JAXBElement("status","Completed"));
		   
		   
		   amdID= update(updateAmd);
		   System.out.println("Downgrade completed(amendment id:"+amdID+").");
		   
		  //query new subscription
		   Subscription newSub_cancel = queryPreviousSubscription(sub.getId().getValue());
		   System.out.println("Subscrption status :"+newSub_cancel.getStatus().getValue());
	   }
	 private Subscription queryPreviousSubscription(String id) throws Exception {
		  QueryResult result =soap.query("SELECT id, PreviousSubscriptionId,name,status,version FROM Subscription WHERE PreviousSubscriptionId = '"+id+"'", header);
	      List<ZObject> list = result.getRecords();
	      Subscription rec=(Subscription)list.get(0);
	      return rec;
	   }
	 private void testUpgradeAndDowngrade()throws Exception{
		   System.out.println("Subscribe(do upgrade and downgrade) call....");
		   //subscribe
		   List<SubscribeResult> result = createSubscribe(Boolean.TRUE);
		   Subscription sub = querySubscription(result.get(0).getSubscriptionId().getValue());
		   System.out.println("\t"+createMessage(result));
		   System.out.println("\tSubscription created:"+ sub.getName().getValue());
		   System.out.println("\tSubscription Version :"+sub.getVersion().getValue());
		   System.out.println("\tSubscription Id :"+sub.getId().getValue());

		   Calendar ca = Calendar.getInstance();
		   
		   //upgrade (add new product)
		   System.out.println("Subscribe(do upgrade) call....");
		   String rpID = createAmendmentWithNewProduct(sub.getId().getValue(),ca);
		   
		   //query new version subscription
		   Subscription newSub_new = queryPreviousSubscription(sub.getId().getValue());
		   System.out.println("\tSubscription Version :"+newSub_new.getVersion().getValue());
		   System.out.println("\tSubscription Id :"+newSub_new.getId().getValue());
		   System.out.println("\tPreviousSubscription Id :"+newSub_new.getPreviousSubscriptionId().getValue());

		   //downgrade (remove new product)
		   System.out.println("Subscribe(do downgrade) call....");
		   createAmendmentWithRemoveProduct(newSub_new.getId().getValue(),rpID,ca);
//		   
		   //query new subscription
		   Subscription newSub_remove = queryPreviousSubscription(newSub_new.getId().getValue());
		   System.out.println("\tSubscription Version :"+newSub_remove.getVersion().getValue());
		   System.out.println("\tSubscription Id :"+newSub_new.getId().getValue());
		   System.out.println("\tPreviousSubscription Id :"+newSub_new.getPreviousSubscriptionId().getValue());

	   }
	 private String createAmendmentWithNewProduct(String subID,Calendar efd)throws Exception{
		   ProductRatePlan prp = getProductRatePlanByProductName(getPropertyValue(PROPERTY_PRODUCT_NAME));
		   //create Amendment 
		   XMLGregorianCalendar effectiveDate=convert(efd);
		   Amendment amd = new Amendment();
		   amd.setName(Custom_JAXBElement("name","Amendment:new Product"));
		   amd.setType(Custom_JAXBElement("type","NewProduct"));
		   amd.setSubscriptionId(Custom_JAXBElement("subscriptionId",subID));
		   amd.setStatus(Custom_JAXBElement("status","Draft"));
		   String amdID= create(amd);
		   //create rate plan
		   RatePlan rp = new RatePlan();
		   rp.setAmendmentId(Custom_JAXBElement("AmendmentId",amdID)); 
		   rp.setAmendmentType(Custom_JAXBElement("AmendmentType",amd.getType().getValue()));
		   rp.setProductRatePlanId(Custom_JAXBElement("ProductRatePlanId",prp.getId().getValue()));
		   String rpID = create(rp);
		   
		   Amendment updateAmd = new Amendment();
		   updateAmd.setId(Custom_JAXBElement("Id",amdID));
		   updateAmd.setContractEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ContractEffectiveDate"),XMLGregorianCalendar.class,effectiveDate));
		   updateAmd.setStatus(Custom_JAXBElement("status","Completed"));
		 
		   amdID = update(updateAmd);
		   System.out.println("\tUpgrade completed(amendment id:"+amdID+")");
		   return rpID;
	   }
	 private String createAmendmentWithRemoveProduct(String subID,String rpID ,Calendar efd)throws Exception{
		   ProductRatePlan prp = getProductRatePlanByProductName(getPropertyValue(PROPERTY_PRODUCT_NAME));
		   //create Amendment 
		   Amendment amd = new Amendment();
		   XMLGregorianCalendar effectiveDate=convert(efd);
		   amd.setName(Custom_JAXBElement("name","Amendment:remove Product"));
		   amd.setType(Custom_JAXBElement("type","RemoveProduct"));
		   amd.setSubscriptionId(Custom_JAXBElement("subscriptionId",subID));
		   amd.setStatus(Custom_JAXBElement("status","Draft"));
		   amd.setEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ContractEffectiveDate"),XMLGregorianCalendar.class,effectiveDate));
		   String amdID= create(amd);
		   
		   //create rate plan
		   RatePlan rp = new RatePlan();
		   rp.setAmendmentId(Custom_JAXBElement("AmendmentId",amdID)); 
		   rp.setAmendmentType(Custom_JAXBElement("AmendmentType",amd.getType().getValue()));
		   rp.setProductRatePlanId(Custom_JAXBElement("ProductRatePlanId",prp.getId().getValue()));
		   rp.setAmendmentSubscriptionRatePlanId(Custom_JAXBElement("AmendmentSubscriptionRatePlanId",rpID));
		   create(rp);
		   
		   Amendment updateAmd = new Amendment();
		   updateAmd.setId(Custom_JAXBElement("Id",amdID));
		   updateAmd.setContractEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ContractEffectiveDate"),XMLGregorianCalendar.class,effectiveDate));
		   updateAmd.setStatus(Custom_JAXBElement("status","Completed"));
		  
		   amdID = update(updateAmd);
		   System.out.println("\tDowngrade completed(amendment id:"+amdID+").");
		   return amdID;
	   }
	 private void testCreatePayment()throws Exception{
		   System.out.println("Create Payment against Invoice....");
		   List<SubscribeResult> result = createSubscribe(Boolean.FALSE);
		   SubscribeResult res=result.get(0);
		   System.out.println("\t"+createMessage(result));
		   Subscription subscription = querySubscription(res.getSubscriptionId().getValue());
		   System.out.println("\tSubscription created:"+ subscription.getName().getValue());
		   
		   String iId = res.getInvoiceId().getValue();
		   String aId = res.getAccountId().getValue();
		   Account account = queryAccount(aId);
		   
		   Payment payment = new Payment();
		   Calendar efd=Calendar.getInstance();
		   XMLGregorianCalendar effectiveDate=convert(efd);
		   payment.setEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","EffectiveDate"),XMLGregorianCalendar.class,effectiveDate));
		   payment.setAccountId(Custom_JAXBElement("accountId",aId));
		   payment.setAmount(new JAXBElement<BigDecimal>(new QName("http://object.api.zuora.com/","amount"),BigDecimal.class,new BigDecimal("1.00")));
		   payment.setPaymentMethodId(Custom_JAXBElement("paymentMethodId",account.getDefaultPaymentMethodId().getValue()));
		   payment.setType(Custom_JAXBElement("type","Electronic"));
		   payment.setStatus(Custom_JAXBElement("status","Draft"));
		   String pId = create(payment);
		   
		   InvoicePayment ip = new InvoicePayment();
		   ip.setAmount(new JAXBElement<BigDecimal>(new QName("http://object.api.zuora.com/","amount"),BigDecimal.class,new BigDecimal(payment.getAmount().getValue().toString())));
//		   ip.setAmount(payment.getAmount());
		   ip.setInvoiceId(Custom_JAXBElement("invoiceId",iId));
		   ip.setPaymentId(Custom_JAXBElement("paymentId",pId));
		   create(ip);
		   Payment updatePayment = new Payment();
		   updatePayment.setId(Custom_JAXBElement("Id",pId));
		   updatePayment.setStatus(Custom_JAXBElement("status","Processed"));
		   pId = update(updatePayment);
		   
		   System.out.println("\n\tPayment created:"+pId);	   
	   }
	 private void testAddUsage()throws Exception{
		   System.out.println("Create Usage....");
		   String aId = createAccount(true);
		   Calendar cal=Calendar.getInstance();
		   XMLGregorianCalendar calendar=convert(cal);
		  
		   Usage usage = new Usage();
		   usage.setAccountId(Custom_JAXBElement("accountId",aId));
		   usage.setQuantity(new JAXBElement<BigDecimal>(new QName("http://object.api.zuora.com/","Quantity"),BigDecimal.class,new BigDecimal("20.0")));
		  
		   usage.setStartDateTime(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","StartDateTime"),XMLGregorianCalendar.class,calendar));
		   
		   usage.setUOM(Custom_JAXBElement("UOM","Each"));
		   String uID = create(usage);
		   
		   System.out.println("\tUsage created:"+uID);
	   }
	 public static void printHelp() {
			StringBuilder buff = new StringBuilder("The commands are:\n\t");
			buff.append("\"ant all\": run all test methods \n\t");
			buff.append("\"ant c-account\": Creates an Active Account  \n\t");
			buff.append("\"ant c-subscribe\": Creates new subscription,one-call \n\t");
			buff.append("\"ant c-subscribe-no-p\": Creates new subscription,one-call,no payments \n\t");
			buff.append("\"ant c-subscribe-w-existingAccount\": Creates new subscription on existing account \n\t");
			buff.append("\"ant c-subscribe-w-amendment\": Creates new subscription ,upgrade and downgrade \n\t");
			buff.append("\"ant cnl-subscription\": Cancel subscription \n\t");
			buff.append("\"ant c-payment\": Creates payment on invoice \n\t");
			buff.append("\"ant c-usage\": Add usage \n\t");
			System.out.println(buff.toString());
		}
}
