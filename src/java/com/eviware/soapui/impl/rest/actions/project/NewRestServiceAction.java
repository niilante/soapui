/*
 *  soapUI, copyright (C) 2004-2008 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.rest.actions.project;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.rest.RestServiceFactory;
import com.eviware.soapui.impl.rest.actions.service.NewRestResourceAction;
import com.eviware.soapui.impl.rest.support.WadlImporter;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.HelpUrls;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.MessageSupport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldListener;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AField.AFieldType;
import com.eviware.x.form.support.AForm;
import com.eviware.x.form.validators.RequiredValidator;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Actions for importing an existing soapUI project file into the current workspace
 * 
 * @author Ole.Matzura
 */

public class NewRestServiceAction extends AbstractSoapUIAction<WsdlProject>
{
	public static final String SOAPUI_ACTION_ID = "NewRestServiceAction"; 
	public static final MessageSupport messages = MessageSupport.getMessages( NewRestServiceAction.class );
	private XFormDialog dialog;
	private final static Logger log = Logger.getLogger( NewRestServiceAction.class );

	public NewRestServiceAction()
   {
      super( messages.get( "title"), messages.get( "description") ); 
   }

	public void perform( WsdlProject project, Object param )
	{
		if( dialog == null )
   	{
			dialog = ADialogBuilder.buildDialog( Form.class );
			dialog.getFormField( Form.SERVICENAME ).addFormFieldValidator( new RequiredValidator( "Service Name is required") );
			dialog.getFormField( Form.SERVICEENDPOINT ).addFormFieldListener( new XFormFieldListener() {

				public void valueChanged(XFormField sourceField, String newValue, String oldValue)
				{
					boolean enable = false;
					
					try
					{
						URL url = new URL( newValue );
						enable = url.getPath().length() > 0 && 
						   !(url.getPath().length() == 1 && url.getPath().charAt(0)=='/');
						
						if( enable )
						{
							newValue = dialog.getValue( Form.WADLURL );
							
							try
							{
								new URL( newValue );
								enable = false;
							}
							catch (MalformedURLException e)
							{
								if( new File( newValue ).exists())
									enable = false;
							}
						}
					}
					catch (MalformedURLException e)
					{
					}
					
					dialog.getFormField(Form.EXTRACTPARAMS).setEnabled(enable);
				}});
			
			dialog.getFormField( Form.WADLURL ).addFormFieldListener( new XFormFieldListener() {

				public void valueChanged(XFormField sourceField, String newValue, String oldValue)
				{
					boolean enable = true;
					
					try
					{
						new URL( newValue );
					}
					catch (MalformedURLException e)
					{
						if( new File( newValue ).exists())
							enable = false;
					}
					
					dialog.getFormField(Form.CREATERESOURCE ).setEnabled(enable);
					
					if( enable )
					{
						try
						{
							URL url = new URL( dialog.getValue(Form.SERVICEENDPOINT) );
							enable = url.getPath().length() > 0 && 
						   	!(url.getPath().length() == 1 && url.getPath().charAt(0)=='/');
						}
						catch (MalformedURLException e)
						{
							enable = false;
						}
						
						dialog.getFormField(Form.EXTRACTPARAMS).setEnabled(enable);
					}
					else dialog.getFormField(Form.EXTRACTPARAMS).setEnabled(false);
				}});
			
			dialog.getFormField(Form.EXTRACTPARAMS).setEnabled(false);
   	}
   	else 
   	{
   		dialog.setValue( Form.SERVICENAME, "" ); 
   		dialog.setValue( Form.SERVICEENDPOINT, "" );
   		dialog.setValue( Form.WADLURL, "" );
   	}
   	
		if( param instanceof ModelItem )
   		dialog.setValue( Form.SERVICENAME, ((ModelItem)param).getName() );
		
   	if( dialog.show() )
   	{
   		RestService restService = (RestService) project.addNewInterface( dialog.getValue(Form.SERVICENAME), RestServiceFactory.REST_TYPE );
   		UISupport.select(restService);
   		URL url = null;
   		
   		try
   		{
   			url = new URL( dialog.getValue(Form.SERVICEENDPOINT));
   			String endpoint = url.getProtocol() + "://" + url.getHost();
   			if( url.getPort() > 0 )
   				endpoint += ":" + url.getPort();
   			
				restService.addEndpoint( endpoint );
				restService.setBasePath(url.getPath());
   		}
   		catch( Exception e )
   		{}
   		
   		if( dialog.getFormField(Form.EXTRACTPARAMS).isEnabled() && dialog.getBooleanValue(Form.EXTRACTPARAMS))
   		{
				restService.setBasePath( "" );
				SoapUI.getActionRegistry().getAction(NewRestResourceAction.SOAPUI_ACTION_ID).perform(restService, url );
   		}
   		
   		String wadl = dialog.getValue(Form.WADLURL);
   		if( StringUtils.hasContent(wadl))
   		{
   			try
				{
					File f = new File( wadl );
               WadlImporter importer = new WadlImporter( restService );
               importer.initFromWadl( f.exists() ? f.toURI().toURL().toString() : wadl );
				}
				catch (Exception e)
				{
					log.error(e.toString());
				}
   		}
   		
   		if( dialog.getFormField(Form.CREATERESOURCE).isEnabled() && dialog.getBooleanValue(Form.CREATERESOURCE))
   		{
   			SoapUI.getActionRegistry().getAction(NewRestResourceAction.SOAPUI_ACTION_ID).perform(restService, null);
   		}
   	}
   }
	
   @AForm( name="Form.Title", description = "Form.Description", helpUrl=HelpUrls.NEWRESTSERVICE_HELP_URL, icon=UISupport.TOOL_ICON_PATH)
	public interface Form 
	{
		@AField( description = "Form.ServiceName.Description", type = AFieldType.STRING ) 
		public final static String SERVICENAME = messages.get("Form.ServiceName.Label"); 
		
		@AField(description = "Form.ServiceUrl.Description", type = AFieldType.STRING ) 
		public final static String SERVICEENDPOINT = messages.get("Form.ServiceUrl.Label"); 

		@AField(description = "Form.ExtractParams.Description", type = AFieldType.BOOLEAN ) 
		public final static String EXTRACTPARAMS = messages.get("Form.ExtractParams.Label"); 

		@AField(description = "Form.WadlUrl.Description", type = AFieldType.FILE ) 
		public final static String WADLURL = messages.get("Form.WadlUrl.Label"); 
		
		@AField(description = "Form.CreateResource.Description", type = AFieldType.BOOLEAN ) 
		public final static String CREATERESOURCE = messages.get("Form.CreateResource.Label"); 

	}
}