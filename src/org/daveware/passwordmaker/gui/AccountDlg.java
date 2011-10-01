/*
 * PasswordMaker Java Edition - One Password To Rule Them All
 * Copyright (C) 2011 Dave Marotti
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.daveware.passwordmaker.gui;

import org.daveware.passwordmaker.Account;
import org.daveware.passwordmaker.AccountPatternData;
import org.daveware.passwordmaker.AlgorithmType;
import org.daveware.passwordmaker.LeetLevel;
import org.daveware.passwordmaker.LeetType;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class AccountDlg {
	Account account = null;
	boolean okClicked = false;
	
	protected Shell shlAccountSettings;
	private Text textName;
	private Text textNotes;
	private Text textUseUrl;
	private Text textUsername;
	private Text textPrefix;
	private Text textSuffix;
	private Text textModifier;
	private Text textPasswordLength;
	private Table tablePatterns;
	private TableViewer tableViewer;
	private Button checkAutoPop;
	private Button btnOk;
	private Button btnCancel;
	private Combo comboUseLeet;
	private Combo comboLeetLevel;
	private CTabFolder tabFolder;
	private Combo comboHashAlgorithm;
	private Combo comboCharacters;
	private CTabItem tbtmUrl;
	private CTabItem tbtmExtended;
	private Composite compositeUrls;
	
	/**
	 * @wbp.parser.constructor
	 * 
	 * This is used purely for WindowBuilder.
	 */
	private AccountDlg() {
		try {
			AccountDlg window = new AccountDlg();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public AccountDlg(Account acc) {
		account = new Account();
		// edit mode
		if(acc!=null) {
    		account.copySettings(acc);
    		account.setId(acc.getId());
		}
	}

	/**
	 */
	public void wbEntryPoint(String[] args) {
		try {
			AccountDlg window = new AccountDlg();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean populateAccountFromGui() {
	    // General page
	    if(textName.getText().trim().length()>0)
	        account.setName(textName.getText());
	    else {
	        return false;
	    }
	    
	    account.setDesc(textNotes.getText());
	    
	    // For folders, the 2 tabs which contain the rest of the items have already been
	    // disposed of.  Use of their widgets will cause SWT exceptions.
	    if(account.isFolder())
	        return true;
	    
	    // URLs page
	    account.setUrl(textUseUrl.getText());

	    // TODO: the table
	    
	    // Extended page
	    account.setUsername(textUsername.getText());
	    switch(comboUseLeet.getSelectionIndex()) {
            case 0: account.setLeetType(LeetType.NONE); break;
            case 1: account.setLeetType(LeetType.BEFORE); break;
            case 2: account.setLeetType(LeetType.AFTER); break;
            case 3: account.setLeetType(LeetType.BOTH); break;
            default: account.setLeetType(LeetType.NONE); break;
	    }
	    account.setLeetLevel(LeetLevel.fromInt(comboLeetLevel.getSelectionIndex()));
	    int selectedAlgo = comboHashAlgorithm.getSelectionIndex();
	    account.setAlgorithm(AlgorithmType.getTypes()[selectedAlgo/2]);
	    account.setHmac((selectedAlgo & 1)!=0);
	    
	    if(textPasswordLength.getText().trim().length()>0) {
    	    int passLength = 0;
    	    try {
    	        passLength = Integer.parseInt(textPasswordLength.getText());
    	    } catch(Exception badPassLen) {
    	        return false;
    	    }
    	    account.setLength(passLength);
	    }
	    else {
	        return false;
	    }
	    
	    if(comboCharacters.getText().length()>2) {
	        account.setCharacterSet(comboCharacters.getText());
	    } else {
	        return false;
	    }
	       
	    account.setModifier(textModifier.getText());
	    account.setPrefix(textPrefix.getText());
	    account.setSuffix(textSuffix.getText());
	    
	    return true;
	}

	private void populateFromAccount() {
		if(account==null)
			return;
		
		// General page
		textName.setText(account.getName());
		textNotes.setText(account.getDesc());
		
		// URLs page
		textUseUrl.setText(account.getUrl());
		// TODO: table
		
		if(account.isFolder()) {
		    shlAccountSettings.setText("Folder Settings");
		    return;
		}
		
		// Extended page
		textUsername.setText(account.getUsername());
		if(account.getLeetType()==LeetType.NONE) {
			comboUseLeet.select(0);
			comboLeetLevel.setEnabled(false);
		}
		else {
			if(account.getLeetType()==LeetType.BEFORE)
				comboUseLeet.select(1);
			else if(account.getLeetType()==LeetType.AFTER)
				comboUseLeet.select(2);
			else if(account.getLeetType()==LeetType.BOTH)
				comboUseLeet.select(3);
			
			comboLeetLevel.setEnabled(true);
			comboLeetLevel.select(account.getLeetLevel().getLevel()-1);
		}
		
		// Populate the algorithms
		for(AlgorithmType type : AlgorithmType.getTypes()) {
			comboHashAlgorithm.add(type.getName());
			comboHashAlgorithm.add("HMAC-" + type.getName());
		}
		comboHashAlgorithm.select((account.getAlgorithm().getType()-1) * 2 +(account.isHmac()?1:0));
		
		textPasswordLength.setText(Integer.toString(account.getLength()));

		comboCharacters.add(account.getCharacterSet(), 0);
		comboCharacters.select(0);

		textModifier.setText(account.getModifier());
		textPrefix.setText(account.getPrefix());
		textSuffix.setText(account.getSuffix());
	}
	
	private void setupPatternTable() {
		tableViewer.setContentProvider(new AccountPatternModel());
		tableViewer.setLabelProvider(new ITableLabelProvider() {
			
			@Override
			public void removeListener(ILabelProviderListener arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean isLabelProperty(Object arg0, String arg1) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public void dispose() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void addListener(ILabelProviderListener arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public String getColumnText(Object arg0, int col) {
				if(arg0 instanceof AccountPatternData) {
					AccountPatternData data = (AccountPatternData)arg0;
					
					switch(col) {
					case 0:
						return data.isEnabled() ? "Yes" : "No";
					case 1:
						return data.getPattern();
					case 2:
						return data.getDesc();
					case 3:
						return data.getType().toString();
					}
				}
				return "WTF?";
			}
			
			@Override
			public Image getColumnImage(Object arg0, int arg1) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		tableViewer.setInput(account);
	}
	
	/**
	 * Open the window.
	 */
	public Account open() {
		Display display = Display.getDefault();
		createContents();
		
		tabFolder.setSelection(0);
		populateFromAccount();
		setupPatternTable();
		
		if(account.isFolder()) {
		    tbtmUrl.dispose();
		    tbtmUrl = null;
		    
		    tbtmExtended.dispose();
		    tbtmExtended = null;
		}
		
		shlAccountSettings.open();
		shlAccountSettings.layout();
		
	
		while (!shlAccountSettings.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		if(okClicked)
		    return account;
		return null;
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		// "SHEET" is working for now, not gonna mess with it. I've heard this makes it arrive on the
		// screen in a mac-way on OSX. On windows this seems to properly make it modal.
		shlAccountSettings = new Shell(Display.getDefault(), SWT.SHEET);
		shlAccountSettings.setSize(728, 501);
		shlAccountSettings.setText("Account Settings");
		shlAccountSettings.setLayout(new FormLayout());
		
		btnOk = new Button(shlAccountSettings, SWT.NONE);
		btnOk.addSelectionListener(new SelectionAdapter() {
		    @Override
		    public void widgetSelected(SelectionEvent arg0) {
		        onOkSelected();
		    }
		});
		btnOk.setImage(SWTResourceManager.getImage(AccountDlg.class, "/org/daveware/passwordmaker/icons/check.png"));
		FormData fd_btnOk = new FormData();
		//fd_btnok.top = new FormAttachment(0, 417);
		fd_btnOk.right = new FormAttachment(100, -10);
		fd_btnOk.width = 90;
		fd_btnOk.bottom = new FormAttachment(100, -10);
		btnOk.setLayoutData(fd_btnOk);
		btnOk.setText("OK");
        shlAccountSettings.setDefaultButton(btnOk);
		
		btnCancel = new Button(shlAccountSettings, SWT.NONE);
		btnCancel.addSelectionListener(new SelectionAdapter() {
		    @Override
		    public void widgetSelected(SelectionEvent arg0) {
		        onCancelSelected();
		    }
		});
		btnCancel.setImage(SWTResourceManager.getImage(AccountDlg.class, "/org/daveware/passwordmaker/icons/cancel.png"));
		FormData fd_btnCancel = new FormData();
		fd_btnCancel.top = new FormAttachment(btnOk, 0, SWT.TOP);
		fd_btnCancel.right = new FormAttachment(btnOk, -6);
		fd_btnCancel.width = 90;
		btnCancel.setLayoutData(fd_btnCancel);
		btnCancel.setText("Cancel");
		
		tabFolder = new CTabFolder(shlAccountSettings, SWT.BORDER);
		FormData fd_tabFolder = new FormData();
		fd_tabFolder.bottom = new FormAttachment(btnOk, -100);
		fd_tabFolder.top = new FormAttachment(0, 10);
		fd_tabFolder.right = new FormAttachment(btnOk, 0, SWT.RIGHT);
		fd_tabFolder.left = new FormAttachment(0, 10);
		tabFolder.setLayoutData(fd_tabFolder);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		
		CTabItem tbtmGeneral = new CTabItem(tabFolder, SWT.NONE);
		tbtmGeneral.setText("General");
		
		Composite composite = new Composite(tabFolder, SWT.NONE);
		tbtmGeneral.setControl(composite);
		composite.setLayout(new GridLayout(2, false));
		
		Label lblName = new Label(composite, SWT.NONE);
		lblName.setAlignment(SWT.RIGHT);
		lblName.setText("Name:");
		
		textName = new Text(composite, SWT.BORDER);
		textName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lblNotes = new Label(composite, SWT.NONE);
		lblNotes.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		lblNotes.setAlignment(SWT.RIGHT);
		lblNotes.setText("Notes:");
		
		textNotes = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		textNotes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		tbtmUrl = new CTabItem(tabFolder, SWT.NONE);
		tbtmUrl.setImage(SWTResourceManager.getImage(AccountDlg.class, "/org/daveware/passwordmaker/icons/world_link.png"));
		tbtmUrl.setText("URLs");
		
		compositeUrls = new Composite(tabFolder, SWT.NONE);
		tbtmUrl.setControl(compositeUrls);
		compositeUrls.setLayout(new GridLayout(2, false));
		
		Label lblUseTheFollowing = new Label(compositeUrls, SWT.NONE);
		lblUseTheFollowing.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		lblUseTheFollowing.setText("Use the following URL/text to calculate the generated password:");
		
		textUseUrl = new Text(compositeUrls, SWT.BORDER);
		textUseUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		checkAutoPop = new Button(compositeUrls, SWT.CHECK);
		checkAutoPop.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		checkAutoPop.setText("Automatically populate username and password fields for sites that match this URL");
		new Label(compositeUrls, SWT.NONE);
		new Label(compositeUrls, SWT.NONE);
		
		Group grpUrlPatterns = new Group(compositeUrls, SWT.NONE);
		grpUrlPatterns.setText("URL Patterns");
		grpUrlPatterns.setLayout(new FillLayout(SWT.HORIZONTAL));
		grpUrlPatterns.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		tableViewer = new TableViewer(grpUrlPatterns, SWT.BORDER | SWT.FULL_SELECTION);
		tablePatterns = tableViewer.getTable();
		tablePatterns.setHeaderVisible(true);
		
		TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnEnabled = tableViewerColumn.getColumn();
		tblclmnEnabled.setWidth(100);
		tblclmnEnabled.setText("Enabled");
		
		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnPattern = tableViewerColumn_1.getColumn();
		tblclmnPattern.setWidth(300);
		tblclmnPattern.setText("Pattern");
		
		TableViewerColumn tableViewerColumn_2 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnPatternName = tableViewerColumn_2.getColumn();
		tblclmnPatternName.setWidth(100);
		tblclmnPatternName.setText("Pattern Name");
		
		TableViewerColumn tableViewerColumn_3 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnPatternType = tableViewerColumn_3.getColumn();
		tblclmnPatternType.setWidth(150);
		tblclmnPatternType.setText("Pattern Type");

		Composite compositePatternButtons = new Composite(compositeUrls, SWT.NONE);
		RowLayout rl_compositePatternButtons = new RowLayout(SWT.HORIZONTAL);
		compositePatternButtons.setLayout(rl_compositePatternButtons);
		compositePatternButtons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Button btnAddPattern = new Button(compositePatternButtons, SWT.NONE);
		btnAddPattern.setText("&Add Pattern");
		
		Button btnEditPattern = new Button(compositePatternButtons, SWT.NONE);
		btnEditPattern.setText("&Edit Pattern");
		
		Button btnCopyPattern = new Button(compositePatternButtons, SWT.NONE);
		btnCopyPattern.setText("&Copy Pattern");
		
		Button btnDeletePattern = new Button(compositePatternButtons, SWT.NONE);
		btnDeletePattern.setText("&Delete Pattern");
		
		tbtmExtended = new CTabItem(tabFolder, SWT.NONE);
		tbtmExtended.setImage(SWTResourceManager.getImage(AccountDlg.class, "/org/daveware/passwordmaker/icons/small_lock.png"));
		tbtmExtended.setText("Extended");
		
		Composite composite_2 = new Composite(tabFolder, SWT.NONE);
		tbtmExtended.setControl(composite_2);
		composite_2.setLayout(new GridLayout(5, false));
		
		Label lblUsername = new Label(composite_2, SWT.NONE);
		lblUsername.setAlignment(SWT.RIGHT);
		lblUsername.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUsername.setText("Username:");
		
		textUsername = new Text(composite_2, SWT.BORDER);
		textUsername.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		
		Label lblUseLt = new Label(composite_2, SWT.NONE);
		lblUseLt.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUseLt.setAlignment(SWT.RIGHT);
		lblUseLt.setText("Use l33t:");
		
		comboUseLeet = new Combo(composite_2, SWT.READ_ONLY);
		comboUseLeet.setItems(new String[] {"not at all", "before generating password", "after generating password", "before and after generating password"});
		comboUseLeet.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboUseLeet.select(0);
		new Label(composite_2, SWT.NONE);
		
		Label lblLtLevel = new Label(composite_2, SWT.NONE);
		lblLtLevel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblLtLevel.setText("l33t Level:");
		
		comboLeetLevel = new Combo(composite_2, SWT.READ_ONLY);
		comboLeetLevel.setEnabled(false);
		comboLeetLevel.setItems(new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9"});
		comboLeetLevel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		comboLeetLevel.select(0);
		
		Label lblHashAlgorithm = new Label(composite_2, SWT.NONE);
		lblHashAlgorithm.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblHashAlgorithm.setAlignment(SWT.RIGHT);
		lblHashAlgorithm.setText("Hash Algorithm:");
		
		comboHashAlgorithm = new Combo(composite_2, SWT.READ_ONLY);
		comboHashAlgorithm.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		
		Label lblPasswordLength = new Label(composite_2, SWT.NONE);
		lblPasswordLength.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPasswordLength.setAlignment(SWT.RIGHT);
		lblPasswordLength.setText("Password Length:");
		
		textPasswordLength = new Text(composite_2, SWT.BORDER);
		textPasswordLength.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		
		Label lblCharacters = new Label(composite_2, SWT.NONE);
		lblCharacters.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblCharacters.setAlignment(SWT.RIGHT);
		lblCharacters.setText("Characters:");
		
		comboCharacters = new Combo(composite_2, SWT.NONE);
		comboCharacters.setItems(new String[] {"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789`~!@#$%^&*()_-+={}|[]\\:\";'<>?,./", "0123456789abcdef", "0123456789", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", "`~!@#$%^&*()_-+={}|[]\\:\";'<>?,./", "Random"});
		GridData gd_comboCharacters = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_comboCharacters.widthHint = 200;
		comboCharacters.setLayoutData(gd_comboCharacters);
		comboCharacters.select(0);
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		
		Label lblModifier = new Label(composite_2, SWT.NONE);
		lblModifier.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblModifier.setAlignment(SWT.RIGHT);
		lblModifier.setText("Modifier:");
		
		textModifier = new Text(composite_2, SWT.BORDER);
		textModifier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		
		Label lblPasswordPrefix = new Label(composite_2, SWT.NONE);
		lblPasswordPrefix.setAlignment(SWT.RIGHT);
		lblPasswordPrefix.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPasswordPrefix.setText("Password Prefix:");
		
		textPrefix = new Text(composite_2, SWT.BORDER);
		textPrefix.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		
		Label lblPasswordSuffix = new Label(composite_2, SWT.NONE);
		lblPasswordSuffix.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPasswordSuffix.setText("Password Suffix:");
		
		textSuffix = new Text(composite_2, SWT.BORDER);
		textSuffix.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);
		
		Group grpPasswordDetails = new Group(shlAccountSettings, SWT.NONE);
		grpPasswordDetails.setText("Password Details");
		grpPasswordDetails.setLayout(new GridLayout(2, false));
		FormData fd_grpPasswordDetails = new FormData();
		fd_grpPasswordDetails.bottom = new FormAttachment(btnOk, -6);
		fd_grpPasswordDetails.top = new FormAttachment(tabFolder, 6);
		fd_grpPasswordDetails.right = new FormAttachment(btnOk, 0, SWT.RIGHT);
		fd_grpPasswordDetails.left = new FormAttachment(0, 10);
		grpPasswordDetails.setLayoutData(fd_grpPasswordDetails);
		
		Label lblPasswordStrength = new Label(grpPasswordDetails, SWT.NONE);
		lblPasswordStrength.setText("Password Strength:");
		
		ProgressBar progressPasswordStrength = new ProgressBar(grpPasswordDetails, SWT.NONE);
		progressPasswordStrength.setMaximum(10);
		progressPasswordStrength.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lblGeneratedPassword = new Label(grpPasswordDetails, SWT.NONE);
		lblGeneratedPassword.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		lblGeneratedPassword.setText("Generated Password:");
		
		Canvas canvas = new Canvas(grpPasswordDetails, SWT.BORDER);
		GridData gd_canvas = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_canvas.heightHint = 33;
		canvas.setLayoutData(gd_canvas);

	}
	
	void onCancelSelected() {
	    okClicked = false;
	    shlAccountSettings.dispose();
	}
	
	void onOkSelected() {
	    if(populateAccountFromGui()) {
	        okClicked = true;
	        shlAccountSettings.dispose();
	    }
	}
}