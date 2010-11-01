package com.zuora.api.sample.jaxws;

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

public class ApiSample {
	
	private static final String PROPERTY_PRODUCT_NAME = "productName";
	private static final String FILE_PROPERTY_NAME = "sample.properties";
	private static final String PROPERTY_USERNAME = "username";
	private static final String PROPERTY_PASSWORD = "password";
	
	private SessionHeader header;
	private ZuoraService service;
	private Soap soap;
	private Properties properties;
	public ApiSample()  {
	        this.service=new ZuoraService();
	        this.soap = service.getSoap();
	}

	public static void main(String[] arg) {

		try {
			ApiSample sample = new ApiSample();
			sample.login();
			if("all".equals(arg[0])){
	        	 sample.sampleCreateAccount();
	        	 System.out.println("");
	        	 sample.sampleSubscribe();
	        	 System.out.println("");
	        	 sample.sampleSubscribeWithNoPayment();
	        	 System.out.println("");
	        	 sample.sampleUpgradeAndDowngrade();
	        	 System.out.println("");
	        	 sample.sampleSubscribeWithExistingAccount();
	        	 System.out.println("");
	        	 sample.sampleCancelSubscription();
	        	 System.out.println("");
	        	 sample.sampleCreatePayment();
	        	 System.out.println("");
	        	 sample.sampleAddUsage();
	         }
	         else if("c-account".equals(arg[0])){
	        	 sample.sampleCreateAccount();
	         }
	         else if("c-subscribe".equals(arg[0])){
	        	sample.sampleSubscribe(); 
	         }
	         else if("c-subscribe-no-p".equals(arg[0])){
	        	 sample.sampleSubscribeWithNoPayment(); 
	         }
	         else if("c-subscribe-w-existingAccount".equals(arg[0])){
	        	 sample.sampleSubscribeWithExistingAccount(); 
	         }
	         else if("c-subscribe-w-amendment".equals(arg[0])){
	        	 sample.sampleUpgradeAndDowngrade();
	         }
	         else if("cnl-subscription".equals(arg[0])){
	        	 sample.sampleCancelSubscription();
	         }
	         else if("c-payment".equals(arg[0])){
	        	 sample.sampleCreatePayment();
	         }
	         else if("c-usage".equals(arg[0])){
	        	 sample.sampleAddUsage();
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
	/*
	    * 8 User case start: 
	    */
	   /*
	    * # CREATE ACTIVE ACCOUNT
		   # method to create an active account. requires that you have:
			#
			#   1.) a gateway setup, 
			#   2.) gateway configured to not verify new credit cards
			#
			# if you want to verify a new credit card, make sure the card info
			# you specify is correct.
	    */
	 private void sampleCreateAccount()throws Exception{
		   System.out.println("Account Create....");
		   String accountId=createAccount(true);
		   System.out.println("Account Created:"+accountId);
	   }
	 
	 /*
	    * # CREATE NEW SUBSCRIPTION, ONE-CALL

		   1. query product catalog for product rate plan (no charge, to simplify)
		   2. subscribe() call with account/contact/payment method (one call)
		   3. query subscription
	   */
	 private void sampleSubscribe() throws Exception {
		   System.out.println("Subscribe call....");
		   /*
		    * create subscription
		    */
		   List<SubscribeResult> result=createSubscribe(Boolean.TRUE);
		   System.out.println(createMessage(result));
		   /*
		    * query subscription which is just created
		    */
		   Subscription sQuery = querySubscription(result.get(0).getSubscriptionId().getValue());
	       System.out.println("Subscription created:"+ sQuery.getName().getValue());
	   }
	 
	 /*
	    * # CREATE NEW SUBSCRIPTION, ONE-CALL, NO PAYMENTS

		   1. query product catalog for product rate plan (no charge, to simplify)
		   2. subscribe call w/ #1 above
		   3. subscribe options processpayments=false
		   4. query subscription
	    */
	 private void sampleSubscribeWithNoPayment()throws Exception{
		   System.out.println("Subscribe(no payments) call....");
		   /*
		    * create subscription without payment
		    */
		   List<SubscribeResult> result = createSubscribe(Boolean.FALSE);
		   System.out.println(createMessage(result));
		   /*
		    * query subscription which is just created
		    */
		   Subscription sQuery = querySubscription(result.get(0).getSubscriptionId().getValue());
	       System.out.println("Subscription created:"+ sQuery.getName().getValue());
	   }
	 
	 /*
	    * # CREATE NEW SUBSCRIPTION, UPGRADE AND DOWNGRADE

		   1. create new order, one-call (#2)
		   2. create amendment w/ new product (upgrade)
		         1. new amendment
		         2. new rate plan, type=NewProduct
		         3. update amendment
		   3. query subscription 
		   4. create amendment w/ remove product (downgrade)
		
		         1. new amendment
		         2. new rate plan, type=RemoveProduct
		         3. update amendment
	    */
	 private void sampleUpgradeAndDowngrade()throws Exception{
		   System.out.println("Subscribe(do upgrade and downgrade) call....");
		   /*
		    * create a new subscribe
		    */
		   List<SubscribeResult> result = createSubscribe(Boolean.TRUE);
		   Subscription sub = querySubscription(result.get(0).getSubscriptionId().getValue());
		   System.out.println("\t"+createMessage(result));
		   System.out.println("\tSubscription created:"+ sub.getName().getValue());
		   System.out.println("\tSubscription Version :"+sub.getVersion().getValue());
		   System.out.println("\tSubscription Id :"+sub.getId().getValue());
		   
		   /*
		    * upgrade (add new product)
		    */
		   Calendar ca = Calendar.getInstance();
		   System.out.println("Subscribe(do upgrade) call....");
		   String rpID = createAmendmentWithNewProduct(sub.getId().getValue(),ca);
		   
		   /*
		    * query new version subscription
		    */
		   Subscription newSub_new = queryPreviousSubscription(sub.getId().getValue());
		   System.out.println("\tSubscription Version :"+newSub_new.getVersion().getValue());
		   System.out.println("\tSubscription Id :"+newSub_new.getId().getValue());
		   System.out.println("\tPreviousSubscription Id :"+newSub_new.getPreviousSubscriptionId().getValue());

		   /*
		    * downgrade (remove new product)
		    */
		   System.out.println("Subscribe(do downgrade) call....");
		   createAmendmentWithRemoveProduct(newSub_new.getId().getValue(),rpID,ca);
		   
		   /*
		    * query new subscription
		    */
		   Subscription newSub_remove = queryPreviousSubscription(newSub_new.getId().getValue());
		   System.out.println("\tSubscription Version :"+newSub_remove.getVersion().getValue());
		   System.out.println("\tSubscription Id :"+newSub_new.getId().getValue());
		   System.out.println("\tPreviousSubscription Id :"+newSub_new.getPreviousSubscriptionId().getValue());

	   }
	 
	 /*
	   # CREATE NEW SUBSCRIPTION ON EXISTING ACCOUNT
	
	   1. create active account
	   2. query product catalog for product rate plan (no charge, to simplify)
	   3. subscribe w/ existing
	   4. query subscription
	*
	*/
	 private void sampleSubscribeWithExistingAccount()throws Exception{
		   /*
		    * create active account
		    */
		   String accountId = createAccount(true);
		   System.out.println("Subscribe(with existing account["+accountId+"]) call....");
		   /*
		    * create Subscribe With Existing Account
		    */
		   List<SubscribeResult> result=createSubscribeWithExistingAccount(accountId);
		   System.out.println(createMessage(result));
		   /*
		    * query the subscription
		    */
		   Subscription sQuery = querySubscription(result.get(0).getSubscriptionId().getValue());
		   System.out.println("Subscription created:"+ sQuery.getName().getValue());
	   }
	 
	 /*
	    * # CANCEL SUBSCRIPTION

		   1. create new order, one-call (#2)
		   2. create amendment w/ remove product
		         1. new amendment
		         2. new rate plan, type=RemoveProduct
		         3. update amendment
		   3. query new subscription
		    */
	 private void sampleCancelSubscription()throws Exception{
		   System.out.println("Cancel Subscribe....");
		   /*
		    * create subsribe
		    */
		   List <SubscribeResult> result = createSubscribe(Boolean.TRUE);
		   System.out.println(createMessage(result));
		   Subscription sub = querySubscription(result.get(0).getSubscriptionId().getValue());
		   System.out.println("Subscription created:"+ sub.getName().getValue());
		   System.out.println("Subscrption status :"+sub.getStatus().getValue());
	       /*
	        * create a  amendment 
	        */
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
		   /*
		    * update amendment
		    */
		   Amendment updateAmd = new Amendment();
		   updateAmd.setId(Custom_JAXBElement("Id",amdID));
		   updateAmd.setContractEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","EffectiveDate"),XMLGregorianCalendar.class,effectiveDate));
		   updateAmd.setStatus(Custom_JAXBElement("status","Completed"));
		   
		   amdID= update(updateAmd);
		   System.out.println("Downgrade completed(amendment id:"+amdID+").");
		   
		  /*
		   * query new subscription
		   */
		   Subscription newSub_cancel = queryPreviousSubscription(sub.getId().getValue());
		   System.out.println("Subscrption status :"+newSub_cancel.getStatus().getValue());
	   }
	 
	 /*
	   # CREATE PAYMENT ON INVOICE
	
	   1. subscribe() call with account/contact/payment method (one call)
	   2. create payment against invoice
*/
	 private void sampleCreatePayment()throws Exception{
		   System.out.println("Create Payment against Invoice....");
		   /*
		    * create subscribe without payment
		    */
		   List<SubscribeResult> result = createSubscribe(Boolean.FALSE);
		   SubscribeResult res=result.get(0);
		   System.out.println("\t"+createMessage(result));
		   Subscription subscription = querySubscription(res.getSubscriptionId().getValue());
		   System.out.println("\tSubscription created:"+ subscription.getName().getValue());
		   
		   String iId = res.getInvoiceId().getValue();
		   String aId = res.getAccountId().getValue();
		   Account account = queryAccount(aId);
		   /*
		    * create payment
		    */
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
		   /*
		    * create invoice payment
		    */
		   InvoicePayment ip = new InvoicePayment();
		   ip.setAmount(new JAXBElement<BigDecimal>(new QName("http://object.api.zuora.com/","amount"),BigDecimal.class,new BigDecimal(payment.getAmount().getValue().toString())));
		   ip.setInvoiceId(Custom_JAXBElement("invoiceId",iId));
		   ip.setPaymentId(Custom_JAXBElement("paymentId",pId));
		   create(ip);
		   /*
		    * update payment
		    */
		   Payment updatePayment = new Payment();
		   updatePayment.setId(Custom_JAXBElement("Id",pId));
		   updatePayment.setStatus(Custom_JAXBElement("status","Processed"));
		   pId = update(updatePayment);
		   
		   System.out.println("\n\tPayment created:"+pId);	   
	   }
	 
	 /**
	  * This object tracks how many units a customer has used of a certain product, such as the number of minutes in a telephone plan. 
	  * @throws Exception
	  */
	 private void sampleAddUsage()throws Exception{
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
	 /*
	  * 8 user case end
	  */
	 
	 
	 /*
	    * 
		   1. create account
		   2. create bill to/sold to
		   3. create default payment method
		   4. update account to active
	    */
	 public String createAccount(boolean active) throws Exception {
		 /*
		  * create a draft account
		  */
	      Account acc1 = createAccount();
	      String accountId = create(acc1);
	      
	      if (active) {
	    	  /*
	    	   * create contact,this is for bill to and send to
	    	   */
	          Contact con = createContact();
	          con.setAccountId(Custom_JAXBElement("accountId",accountId));
	          String contactId = create(con);
	           /*
	           * create default payment method
	           */
	          PaymentMethod pm = createPaymentMethod();
	          pm.setAccountId(Custom_JAXBElement("accountId",accountId));
	          String pmId = create(pm);

	          //
	          /*
	           * set required active fields and activate,update account to active
	           */
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
	 //create draft account
	 public Account  createAccount() throws LoginFault, UnexpectedErrorFault, InvalidTypeFault {
			// create account
		    long time = System.currentTimeMillis();       
			Account acc = new Account();
			acc.setAccountNumber(Custom_JAXBElement("accountNumber","t-" + time));
			acc.setBatch(Custom_JAXBElement("batch","Batch1"));
			acc.setBillCycleDay(2);
			acc.setAllowInvoiceEdit(new JAXBElement<Boolean>(new QName("http://object.api.zuora.com/","allowInvoiceEdit"),Boolean.class,true));
			acc.setAutoPay(new JAXBElement<Boolean>(new QName("http://object.api.zuora.com/","autoPay"),Boolean.class,false));
			acc.setCrmId(Custom_JAXBElement("crmId","SFDC-" + time));
			acc.setCurrency(Custom_JAXBElement("currency","USD"));
			acc.setCustomerServiceRepName(Custom_JAXBElement("customerServiceRepName","CSR Dude"));
			acc.setName(Custom_JAXBElement("name","someAccount" + time));
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
	
	//create contact
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
	      con.setWorkEmail(Custom_JAXBElement("workEmail","contact@sample.com"));
	      con.setWorkPhone(Custom_JAXBElement("workPhone","4152225151"));
	     
	      return con;
	  }
	 //create payment method
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
			//send the request through soap
			List<SaveResult> result = soap.create(list, header);
			//if it failed
			if(!result.get(0).isSuccess())
				System.out.println(result.get(0).getErrors().get(0).getMessage().getValue());
			//get the object id which is just created
			String id=result.get(0).getId().getValue();
			
			
			return id;


	      
	   }
	 /**
	  * Create subscribe.When a custmer buy a product,it produces a subscription.
	  * @param isProcessPayment with payment or not
	  * @return Subscribe result list
	  * @throws Exception
	  */ 
	 private List<SubscribeResult> createSubscribe(boolean isProcessPayment)throws Exception{
		  /*
		   * query product catalog for product rate plan (no charge, to simplify)
		   */
		  ProductRatePlan prp = getProductRatePlanByProductName(getPropertyValue(PROPERTY_PRODUCT_NAME));
		  /*
		   * create subscribe  with account/contact/payment method 
		   */
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
	      /*
	       * setup SubscribeRequest
	       */
	      SubscribeRequest sub = new SubscribeRequest();
	      sub.setAccount(new JAXBElement<Account>(new QName("http://api.zuora.com/","Account"),Account.class,acc));
	      sub.setBillToContact(new JAXBElement<Contact>(new QName("http://api.zuora.com/","BillToContact"),Contact.class,con));
	      sub.setPaymentMethod(new JAXBElement<PaymentMethod>(new QName("http://api.zuora.com/","PaymentMethod"),PaymentMethod.class,pm));
	      sub.setSubscriptionData(new JAXBElement<SubscriptionData>(new QName("http://api.zuora.com/","SubscriptionData"),SubscriptionData.class,sd));
	      sub.setSubscribeOptions(new JAXBElement<SubscribeOptions>(new QName("http://api.zuora.com/","SubscribeOptions"),SubscribeOptions.class,sp));
	      
	      List<SubscribeRequest> subscribes=new ArrayList<SubscribeRequest>();
	      subscribes.add(sub);
	      //send subsribe request to server through soap
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
	      sub.setNotes(Custom_JAXBElement("notes","This is a sample subscription"));

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
		    StringBuilder resultString = new StringBuilder("SusbscribeResult :\n");
		    List<ZObject> list=new ArrayList<ZObject>();
			list.add(acc);
			//send update request through soap
			List<SaveResult> results = soap.update(list, header);
			//get the object id ,which is just updated
			SaveResult result=results.get(0);
			 List<Error> errors = result.getErrors();
	            if (errors != null) {
	               for (Error error : errors) {
	                  resultString.append("\n\tError Code: ").append(error.getCode().getValue())
	                            .append("\n\tError Message: ").append(error.getMessage().getValue());                   
	               }
	            }
	            System.out.println(resultString);
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
	         } else {
	            resultString.append("\nSubscribe Failure Result: \n");
	            List<Error> errors = result.getErrors();
	            if (errors != null) {
	               for (Error error : errors) {
	                  resultString.append("\n\tError Code: ").append(error.getCode().getValue())
	                            .append("\n\tError Message: ").append(error.getMessage().getValue());                   
	               }
	            }
	         }
	      }
	      return resultString.toString();
	   }
	
	 /**
	  * Create an account. 
	  * @param active Whether the account is active or draft.
	  * When it is true ,you can use the account to subscribe,otherwise you don't have this authority.
	  * @return the account id
	  * @throws Exception
	  */
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
	
	 private Subscription queryPreviousSubscription(String id) throws Exception {
		  QueryResult result =soap.query("SELECT id, PreviousSubscriptionId,name,status,version FROM Subscription WHERE PreviousSubscriptionId = '"+id+"'", header);
	      List<ZObject> list = result.getRecords();
	      Subscription rec=(Subscription)list.get(0);
	      return rec;
	   }
	 
	 /**
	  * Add new product.It is used when a customer wants to buy another product.
	  * @param 
	  * @throws Exception
	  */ 
	 private String createAmendmentWithNewProduct(String subID,Calendar efd)throws Exception{
		   ProductRatePlan prp = getProductRatePlanByProductName(getPropertyValue(PROPERTY_PRODUCT_NAME));
		   /*
		    * create Amendment 
		    */
		   XMLGregorianCalendar effectiveDate=convert(efd);
		   Amendment amd = new Amendment();
		   amd.setName(Custom_JAXBElement("name","Amendment:new Product"));
		   amd.setType(Custom_JAXBElement("type","NewProduct"));
		   amd.setSubscriptionId(Custom_JAXBElement("subscriptionId",subID));
		   amd.setStatus(Custom_JAXBElement("status","Draft"));
		   String amdID= create(amd);
		   /*
		    * create rate plan
		    */
		   RatePlan rp = new RatePlan();
		   rp.setAmendmentId(Custom_JAXBElement("AmendmentId",amdID)); 
		   rp.setAmendmentType(Custom_JAXBElement("AmendmentType",amd.getType().getValue()));
		   rp.setProductRatePlanId(Custom_JAXBElement("ProductRatePlanId",prp.getId().getValue()));
		   String rpID = create(rp);
		   /*
		    * update Amendment
		    */
		   Amendment updateAmd = new Amendment();
		   updateAmd.setId(Custom_JAXBElement("Id",amdID));
		   updateAmd.setContractEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ContractEffectiveDate"),XMLGregorianCalendar.class,effectiveDate));
		   updateAmd.setStatus(Custom_JAXBElement("status","Completed"));
		   updateAmd.setServiceActivationDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ServiceActivationDate"),XMLGregorianCalendar.class,effectiveDate));
		   amdID = update(updateAmd);
		   System.out.println("\tUpgrade completed(amendment id:"+amdID+")");
		   return rpID;
	   }
	 /**
	  * Remove product.If the customer don't want to buy a  purchased product,he can remove it.
	  * @param subID,efd subscription id and the calendar
	  * @throws Exception
	  */ 
	 private String createAmendmentWithRemoveProduct(String subID,String rpID ,Calendar efd)throws Exception{
		   ProductRatePlan prp = getProductRatePlanByProductName(getPropertyValue(PROPERTY_PRODUCT_NAME));
		   /*
		    * create Amendment 
		    */
		   Amendment amd = new Amendment();
		   XMLGregorianCalendar effectiveDate=convert(efd);
		   amd.setName(Custom_JAXBElement("name","Amendment:remove Product"));
		   amd.setType(Custom_JAXBElement("type","RemoveProduct"));
		   amd.setSubscriptionId(Custom_JAXBElement("subscriptionId",subID));
		   amd.setStatus(Custom_JAXBElement("status","Draft"));
		   amd.setEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ContractEffectiveDate"),XMLGregorianCalendar.class,effectiveDate));

		   String amdID= create(amd);
		   
		   /*
		    * create rate plan
		    */
		   RatePlan rp = new RatePlan();
		   rp.setAmendmentId(Custom_JAXBElement("AmendmentId",amdID)); 
		   rp.setAmendmentType(Custom_JAXBElement("AmendmentType",amd.getType().getValue()));
		   rp.setProductRatePlanId(Custom_JAXBElement("ProductRatePlanId",prp.getId().getValue()));
		   rp.setAmendmentSubscriptionRatePlanId(Custom_JAXBElement("AmendmentSubscriptionRatePlanId",rpID));
		   create(rp);
		   
		   /*
		    * update Amendment
		    */
		   Amendment updateAmd = new Amendment();
		   updateAmd.setId(Custom_JAXBElement("Id",amdID));
		   updateAmd.setContractEffectiveDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ContractEffectiveDate"),XMLGregorianCalendar.class,effectiveDate));
		   updateAmd.setStatus(Custom_JAXBElement("status","Completed"));
		   updateAmd.setServiceActivationDate(new JAXBElement<XMLGregorianCalendar>(new QName("http://object.api.zuora.com/","ServiceActivationDate"),XMLGregorianCalendar.class,effectiveDate));

		   amdID = update(updateAmd);
		   System.out.println("\tDowngrade completed(amendment id:"+amdID+").");
		   return amdID;
	   }
	
	 public static void printHelp() {
			StringBuilder buff = new StringBuilder("The commands are:\n\t");
			buff.append("\"ant all\": run all sample methods \n\t");
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
