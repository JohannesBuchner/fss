package com.doublesignal.sepm.jake.gui.i18n;

import com.doublesignal.sepm.jake.gui.i18n.exceptions.IllegalNumberOfArgumentsException;
import junit.framework.TestCase;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class I18nTest extends TestCase {
	public void testi18n() throws Exception{
		BeanFactory factory = new XmlBeanFactory(new ClassPathResource("/beans.xml"));
        ITranslationProvider translator = (ITranslationProvider) 
        	factory.getBean("translationProvider");
        
        translator.setLanguage("lang-test.txt");
		assertEquals(translator.get("HELLO_WORLD"),"Hallo Welt!");
		assertEquals(translator.get("HELLO", "Chris"),
				"Hallo Chris!");
		assertEquals(translator.get("HELLO", "Franz"),
				"Hallo Franz!");
		assertEquals(translator.get("NETWORK_TIMEOUT_ON_USER", 
				"chris@doublesignal.com", Integer.toString(100)),
			"Die Netzwerkverbindung zu chris@doublesignal.com brach nach 100 Sekunden ab.");
		
		try{
			translator.get("NETWORK_TIMEOUT_ON_USER");
			fail("IllegalNumberOfArgumentsException");
		}catch (IllegalNumberOfArgumentsException e) {
		}
		try{
			translator.get("NETWORK_TIMEOUT_ON_USER", "chris@doublesignal.com");
			fail("IllegalNumberOfArgumentsException");
		}catch (IllegalNumberOfArgumentsException e) {
		}
		try{
			translator.get("HELLO");
			fail("IllegalNumberOfArgumentsException");
		}catch (IllegalNumberOfArgumentsException e) {
		}
		
		assertEquals(translator.get("FOO_BAR_DO_NOT_KNOW"), "FOO_BAR_DO_NOT_KNOW");
		assertEquals(translator.get("FOO_BAR_DO_NOT_KNOW", "foo", "bar"), "FOO_BAR_DO_NOT_KNOW");
	}
	
}