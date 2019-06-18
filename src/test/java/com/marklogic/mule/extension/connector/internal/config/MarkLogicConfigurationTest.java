package com.marklogic.mule.extension.connector.internal.config;

import com.marklogic.client.document.ServerTransform;
import com.marklogic.mule.extension.connector.internal.exception.MarkLogicConnectorException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jshingle
 */
public class MarkLogicConfigurationTest
{
    MarkLogicConfiguration instance;
    
    @Before
    public void setUp() throws Exception
    {
        instance = new MarkLogicConfiguration();
    }
    
    /**
     * Test of getConfigId method, of class MarkLogicConfiguration.
     */
    @Test
    public void testGetConfigId()
    {
        String expResult = "configuration-id-test-123";
        instance.setConfigId(expResult);
        String result = instance.getConfigId();
        assertEquals(expResult, result);
    }

    /**
     * Test of getThreadCount method, of class MarkLogicConfiguration.
     */
    @Test
    public void testGetThreadCount()
    {
        int expResult = 64;
        instance.setThreadCount(expResult);
        int result = instance.getThreadCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of getBatchSize method, of class MarkLogicConfiguration.
     */
    @Test
    public void testGetBatchSize()
    {
        int expResult = 250;
        instance.setBatchSize(expResult);
        int result = instance.getBatchSize();
        assertEquals(expResult, result);
    }

    /**
     * Test of getServerTransform method, of class MarkLogicConfiguration.
     */
    @Test
    public void testGetServerTransform()
    {
        String expResult = "TestTransform";
        instance.setServerTransform(expResult);
        String result = instance.getServerTransform();
        assertEquals(expResult, result);
    }

    /**
     * Test of getServerTransformParams method, of class MarkLogicConfiguration.
     */
    @Test
    public void testGetServerTransformParams()
    {
        String expResult = "entity-name,MyEntity,flow-name,loadMyEntity";
        instance.setServerTransformParams(expResult);
        String result = instance.getServerTransformParams();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSecondsBeforeFlush method, of class MarkLogicConfiguration.
     */
    @Test
    public void testGetSecondsBeforeFlush()
    {
        int expResult = 2;
        instance.setSecondsBeforeFlush(expResult);
        int result = instance.getSecondsBeforeFlush();
        assertEquals(expResult, result);
    }

    /**
     * Test of getJobName method, of class MarkLogicConfiguration.
     */
    @Test
    public void testGetJobName()
    {
        String expResult = "TestJobName";
        instance.setJobName(expResult);
        String result = instance.getJobName();
        assertEquals(expResult, result);
    }

    /**
     * Test of hasServerTransform method, of class MarkLogicConfiguration.
     */
    @Test
    public void testHasServerTransformNull()
    {
        assertNull(instance.getServerTransform());
        assertFalse(instance.hasServerTransform());
    }
    
    @Test
    public void testHasServerTransformEmptyString()
    {
        String expected = "";
        instance.setServerTransform(expected);
        assertEquals(expected, instance.getServerTransform());
        assertFalse(instance.hasServerTransform());
    }
    
    @Test
    public void testHasServerTransformBlankString()
    {
        String expected = " "; //In case a user enters a space/tab
        instance.setServerTransform(expected);
        assertEquals(expected, instance.getServerTransform());
        assertFalse(instance.hasServerTransform());
    }
    
    @Test
    public void testHasServerTransformNullString()
    {
        String expected = "null";
        instance.setServerTransform(expected);
        assertEquals(expected, instance.getServerTransform());
        assertFalse(instance.hasServerTransform());
    }
    
    @Test
    public void testHasServerTransformNullCapString()
    {
        String expected = "NULL";
        instance.setServerTransform(expected);
        assertEquals(expected, instance.getServerTransform());
        assertFalse(instance.hasServerTransform());
    }
    
    @Test
    public void testHasServerTransform()
    {
        String expected = "TestTransformName";
        instance.setServerTransform(expected);
        assertEquals(expected, instance.getServerTransform());
        assertTrue(instance.hasServerTransform());
    }

    /**
     * Test of createServerTransform method, of class MarkLogicConfiguration.
     */
    @Test(expected = MarkLogicConnectorException.class)
    public void testCreateServerTransformWithoutName()
    {
        instance.createServerTransform();
    }
    
    @Test(expected = MarkLogicConnectorException.class)
    public void testCreateServerTransformWithoutParams()
    {
        instance.setServerTransform("TestTransform");
        
        instance.createServerTransform();
    }
    
    @Test(expected = MarkLogicConnectorException.class)
    public void testCreateServerTransformWithEmptyParams()
    {
        instance.setServerTransform("TestTransform");
        instance.setServerTransformParams("   ");
        instance.createServerTransform();
    }
    
    @Test(expected = MarkLogicConnectorException.class)
    public void testCreateServerTransformWithNullParams()
    {
        instance.setServerTransform("TestTransform");
        instance.setServerTransformParams("null");
        instance.createServerTransform();
    }
    
    
    @Test(expected = MarkLogicConnectorException.class)
    public void testCreateServerTransformUnequalPairs()
    {
        instance.setServerTransform("TestTransform");
        instance.setServerTransformParams("entity-name,MyEntity,flow-name");
        
        instance.createServerTransform();
    }
    
    @Test(expected = MarkLogicConnectorException.class)
    public void testCreateServerTransformUnequalPairs2()
    {
        instance.setServerTransform("TestTransform");
        instance.setServerTransformParams("entity-name,MyEntity,flow-name, ");
        
        instance.createServerTransform();
    }
    
    @Test
    public void testCreateServerTransform()
    {
        createServerTransformTester("TestTransform", "entity-name,MyEntity,flow-name,loadMyEntity");
    }

    @Test
    public void testCreateServerTransformWithSpaces()
    {
        createServerTransformTester("TestTransform", "entity-name, MyEntity, flow-name, loadMyEntity ");
    }
    
    private void createServerTransformTester(String name, String params)
    {
        instance.setServerTransform(name);
        instance.setServerTransformParams(params);
        
        ServerTransform transform = instance.createServerTransform();
        assertEquals(name, transform.getName());
        
        transformParamTester(transform, "entity-name", "MyEntity");
        transformParamTester(transform, "flow-name", "loadMyEntity");
    }
    
    private void transformParamTester(ServerTransform transform, String key, String value)
    {
        assertTrue(transform.containsKey(key));
        
        List<String> list = transform.get(key);
        assertEquals(1, list.size());
        assertEquals(value, list.get(0));
    }
}
