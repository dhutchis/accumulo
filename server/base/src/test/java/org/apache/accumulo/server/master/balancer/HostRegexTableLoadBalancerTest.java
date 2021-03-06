/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.server.master.balancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.thrift.TKeyExtent;
import org.apache.accumulo.core.master.thrift.TabletServerStatus;
import org.apache.accumulo.core.tabletserver.thrift.TabletStats;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.server.conf.ServerConfiguration;
import org.apache.accumulo.server.conf.TableConfiguration;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.master.state.TabletMigration;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;

public class HostRegexTableLoadBalancerTest extends BaseHostRegexTableLoadBalancerTest {

  @Test
  public void testInit() {
    init((ServerConfiguration) factory);
    Assert.assertEquals("OOB check interval value is incorrect", 10000, this.getOobCheckMillis());
    Assert.assertEquals("Pool check interval value is incorrect", 30000, this.getPoolRecheckMillis());
    Assert.assertFalse(isIpBasedRegex());
    Map<String,Pattern> patterns = this.getPoolNameToRegexPattern();
    Assert.assertEquals(2, patterns.size());
    Assert.assertTrue(patterns.containsKey(FOO.getTableName()));
    Assert.assertEquals(Pattern.compile("r01.*").pattern(), patterns.get(FOO.getTableName()).pattern());
    Assert.assertTrue(patterns.containsKey(BAR.getTableName()));
    Assert.assertEquals(Pattern.compile("r02.*").pattern(), patterns.get(BAR.getTableName()).pattern());
    Map<String,String> tids = this.getTableIdToTableName();
    Assert.assertEquals(3, tids.size());
    Assert.assertTrue(tids.containsKey(FOO.getId()));
    Assert.assertEquals(FOO.getTableName(), tids.get(FOO.getId()));
    Assert.assertTrue(tids.containsKey(BAR.getId()));
    Assert.assertEquals(BAR.getTableName(), tids.get(BAR.getId()));
    Assert.assertTrue(tids.containsKey(BAZ.getId()));
    Assert.assertEquals(BAZ.getTableName(), tids.get(BAZ.getId()));
    Assert.assertEquals(false, this.isIpBasedRegex());
  }

  @Test
  public void testBalanceWithMigrations() {
    List<TabletMigration> migrations = new ArrayList<>();
    init((ServerConfiguration) factory);
    long wait = this.balance(Collections.unmodifiableSortedMap(createCurrent(2)), Collections.singleton(new KeyExtent()), migrations);
    Assert.assertEquals(5000, wait);
    Assert.assertEquals(0, migrations.size());
  }

  @Test
  public void testSplitCurrentByRegexUsingHostname() {
    init((ServerConfiguration) factory);
    Map<String,SortedMap<TServerInstance,TabletServerStatus>> groups = this.splitCurrentByRegex(createCurrent(15));
    Assert.assertEquals(3, groups.size());
    Assert.assertTrue(groups.containsKey(FOO.getTableName()));
    SortedMap<TServerInstance,TabletServerStatus> fooHosts = groups.get(FOO.getTableName());
    Assert.assertEquals(5, fooHosts.size());
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.1:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.2:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.3:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.4:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.5:9997", 1)));
    Assert.assertTrue(groups.containsKey(BAR.getTableName()));
    SortedMap<TServerInstance,TabletServerStatus> barHosts = groups.get(BAR.getTableName());
    Assert.assertEquals(5, barHosts.size());
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.6:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.7:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.8:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.9:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.10:9997", 1)));
    Assert.assertTrue(groups.containsKey(DEFAULT_POOL));
    SortedMap<TServerInstance,TabletServerStatus> defHosts = groups.get(DEFAULT_POOL);
    Assert.assertEquals(5, defHosts.size());
    Assert.assertTrue(defHosts.containsKey(new TServerInstance("192.168.0.11:9997", 1)));
    Assert.assertTrue(defHosts.containsKey(new TServerInstance("192.168.0.12:9997", 1)));
    Assert.assertTrue(defHosts.containsKey(new TServerInstance("192.168.0.13:9997", 1)));
    Assert.assertTrue(defHosts.containsKey(new TServerInstance("192.168.0.14:9997", 1)));
    Assert.assertTrue(defHosts.containsKey(new TServerInstance("192.168.0.15:9997", 1)));
  }

  @Test
  public void testSplitCurrentByRegexUsingOverlappingPools() {
    init((ServerConfiguration) new TestServerConfigurationFactory(instance) {
      @Override
      public TableConfiguration getTableConfiguration(String tableId) {
        return new TableConfiguration(getInstance(), tableId, null) {
          HashMap<String,String> tableProperties = new HashMap<>();
          {
            tableProperties.put(HostRegexTableLoadBalancer.HOST_BALANCER_PREFIX + FOO.getTableName(), "r.*");
            tableProperties.put(HostRegexTableLoadBalancer.HOST_BALANCER_PREFIX + BAR.getTableName(), "r01.*|r02.*");
          }

          @Override
          public String get(Property property) {
            return tableProperties.get(property.name());
          }

          @Override
          public void getProperties(Map<String,String> props, Predicate<String> filter) {
            for (Entry<String,String> e : tableProperties.entrySet()) {
              if (filter.test(e.getKey())) {
                props.put(e.getKey(), e.getValue());
              }
            }
          }
        };
      }
    });
    Map<String,SortedMap<TServerInstance,TabletServerStatus>> groups = this.splitCurrentByRegex(createCurrent(15));
    Assert.assertEquals(2, groups.size());
    Assert.assertTrue(groups.containsKey(FOO.getTableName()));
    SortedMap<TServerInstance,TabletServerStatus> fooHosts = groups.get(FOO.getTableName());
    Assert.assertEquals(15, fooHosts.size());
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.1:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.2:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.3:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.4:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.5:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.6:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.7:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.8:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.9:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.10:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.11:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.12:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.13:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.14:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.15:9997", 1)));
    Assert.assertTrue(groups.containsKey(BAR.getTableName()));
    SortedMap<TServerInstance,TabletServerStatus> barHosts = groups.get(BAR.getTableName());
    Assert.assertEquals(10, barHosts.size());
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.1:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.2:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.3:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.4:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.5:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.6:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.7:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.8:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.9:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.10:9997", 1)));
  }

  @Test
  public void testSplitCurrentByRegexUsingIP() {
    init((ServerConfiguration) new TestServerConfigurationFactory(instance) {
      @Override
      public synchronized AccumuloConfiguration getConfiguration() {
        HashMap<String,String> props = new HashMap<>();
        props.put(HostRegexTableLoadBalancer.HOST_BALANCER_OOB_CHECK_KEY, "30s");
        props.put(HostRegexTableLoadBalancer.HOST_BALANCER_POOL_RECHECK_KEY, "30s");
        props.put(HostRegexTableLoadBalancer.HOST_BALANCER_REGEX_USING_IPS_KEY, "true");
        return new ConfigurationCopy(props);
      }

      @Override
      public TableConfiguration getTableConfiguration(String tableId) {
        return new TableConfiguration(getInstance(), tableId, null) {
          HashMap<String,String> tableProperties = new HashMap<>();
          {
            tableProperties.put(HostRegexTableLoadBalancer.HOST_BALANCER_PREFIX + FOO.getTableName(), "192\\.168\\.0\\.[1-5]");
            tableProperties.put(HostRegexTableLoadBalancer.HOST_BALANCER_PREFIX + BAR.getTableName(), "192\\.168\\.0\\.[6-9]|192\\.168\\.0\\.10");
          }

          @Override
          public String get(Property property) {
            return tableProperties.get(property.name());
          }

          @Override
          public void getProperties(Map<String,String> props, Predicate<String> filter) {
            for (Entry<String,String> e : tableProperties.entrySet()) {
              if (filter.test(e.getKey())) {
                props.put(e.getKey(), e.getValue());
              }
            }
          }
        };
      }
    });
    Assert.assertTrue(isIpBasedRegex());
    Map<String,SortedMap<TServerInstance,TabletServerStatus>> groups = this.splitCurrentByRegex(createCurrent(15));
    Assert.assertEquals(3, groups.size());
    Assert.assertTrue(groups.containsKey(FOO.getTableName()));
    SortedMap<TServerInstance,TabletServerStatus> fooHosts = groups.get(FOO.getTableName());
    Assert.assertEquals(5, fooHosts.size());
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.1:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.2:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.3:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.4:9997", 1)));
    Assert.assertTrue(fooHosts.containsKey(new TServerInstance("192.168.0.5:9997", 1)));
    Assert.assertTrue(groups.containsKey(BAR.getTableName()));
    SortedMap<TServerInstance,TabletServerStatus> barHosts = groups.get(BAR.getTableName());
    Assert.assertEquals(5, barHosts.size());
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.6:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.7:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.8:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.9:9997", 1)));
    Assert.assertTrue(barHosts.containsKey(new TServerInstance("192.168.0.10:9997", 1)));
    Assert.assertTrue(groups.containsKey(DEFAULT_POOL));
    SortedMap<TServerInstance,TabletServerStatus> defHosts = groups.get(DEFAULT_POOL);
    Assert.assertEquals(5, defHosts.size());
    Assert.assertTrue(defHosts.containsKey(new TServerInstance("192.168.0.11:9997", 1)));
    Assert.assertTrue(defHosts.containsKey(new TServerInstance("192.168.0.12:9997", 1)));
    Assert.assertTrue(defHosts.containsKey(new TServerInstance("192.168.0.13:9997", 1)));
    Assert.assertTrue(defHosts.containsKey(new TServerInstance("192.168.0.14:9997", 1)));
    Assert.assertTrue(defHosts.containsKey(new TServerInstance("192.168.0.15:9997", 1)));
  }

  @Test
  public void testAllUnassigned() {
    init((ServerConfiguration) factory);
    Map<KeyExtent,TServerInstance> assignments = new HashMap<>();
    Map<KeyExtent,TServerInstance> unassigned = new HashMap<>();
    for (List<KeyExtent> extents : tableExtents.values()) {
      for (KeyExtent ke : extents) {
        unassigned.put(ke, null);
      }
    }
    this.getAssignments(Collections.unmodifiableSortedMap(allTabletServers), Collections.unmodifiableMap(unassigned), assignments);
    Assert.assertEquals(15, assignments.size());
    // Ensure unique tservers
    for (Entry<KeyExtent,TServerInstance> e : assignments.entrySet()) {
      for (Entry<KeyExtent,TServerInstance> e2 : assignments.entrySet()) {
        if (e.getKey().equals(e2.getKey())) {
          continue;
        }
        if (e.getValue().equals(e2.getValue())) {
          Assert.fail("Assignment failure");
        }
      }
    }
    // Ensure assignments are correct
    for (Entry<KeyExtent,TServerInstance> e : assignments.entrySet()) {
      if (!tabletInBounds(e.getKey(), e.getValue())) {
        Assert.fail("tablet not in bounds: " + e.getKey() + " -> " + e.getValue().host());
      }
    }
  }

  @Test
  public void testAllAssigned() {
    init((ServerConfiguration) factory);
    Map<KeyExtent,TServerInstance> assignments = new HashMap<>();
    Map<KeyExtent,TServerInstance> unassigned = new HashMap<>();
    this.getAssignments(Collections.unmodifiableSortedMap(allTabletServers), Collections.unmodifiableMap(unassigned), assignments);
    Assert.assertEquals(0, assignments.size());
  }

  @Test
  public void testPartiallyAssigned() {
    init((ServerConfiguration) factory);
    Map<KeyExtent,TServerInstance> assignments = new HashMap<>();
    Map<KeyExtent,TServerInstance> unassigned = new HashMap<>();
    int i = 0;
    for (List<KeyExtent> extents : tableExtents.values()) {
      for (KeyExtent ke : extents) {
        if ((i % 2) == 0) {
          unassigned.put(ke, null);
        }
        i++;
      }
    }
    this.getAssignments(Collections.unmodifiableSortedMap(allTabletServers), Collections.unmodifiableMap(unassigned), assignments);
    Assert.assertEquals(unassigned.size(), assignments.size());
    // Ensure unique tservers
    for (Entry<KeyExtent,TServerInstance> e : assignments.entrySet()) {
      for (Entry<KeyExtent,TServerInstance> e2 : assignments.entrySet()) {
        if (e.getKey().equals(e2.getKey())) {
          continue;
        }
        if (e.getValue().equals(e2.getValue())) {
          Assert.fail("Assignment failure");
        }
      }
    }
    // Ensure assignments are correct
    for (Entry<KeyExtent,TServerInstance> e : assignments.entrySet()) {
      if (!tabletInBounds(e.getKey(), e.getValue())) {
        Assert.fail("tablet not in bounds: " + e.getKey() + " -> " + e.getValue().host());
      }
    }
  }

  @Test
  public void testUnassignedWithNoTServers() {
    init((ServerConfiguration) factory);
    Map<KeyExtent,TServerInstance> assignments = new HashMap<>();
    Map<KeyExtent,TServerInstance> unassigned = new HashMap<>();
    for (KeyExtent ke : tableExtents.get(BAR.getTableName())) {
      unassigned.put(ke, null);
    }
    SortedMap<TServerInstance,TabletServerStatus> current = createCurrent(15);
    // Remove the BAR tablet servers from current
    List<TServerInstance> removals = new ArrayList<TServerInstance>();
    for (Entry<TServerInstance,TabletServerStatus> e : current.entrySet()) {
      if (e.getKey().host().equals("192.168.0.6") || e.getKey().host().equals("192.168.0.7") || e.getKey().host().equals("192.168.0.8")
          || e.getKey().host().equals("192.168.0.9") || e.getKey().host().equals("192.168.0.10")) {
        removals.add(e.getKey());
      }
    }
    for (TServerInstance r : removals) {
      current.remove(r);
    }
    this.getAssignments(Collections.unmodifiableSortedMap(allTabletServers), Collections.unmodifiableMap(unassigned), assignments);
    Assert.assertEquals(unassigned.size(), assignments.size());
    // Ensure assignments are correct
    for (Entry<KeyExtent,TServerInstance> e : assignments.entrySet()) {
      if (!tabletInBounds(e.getKey(), e.getValue())) {
        Assert.fail("tablet not in bounds: " + e.getKey() + " -> " + e.getValue().host());
      }
    }
  }

  @Test
  public void testOutOfBoundsTablets() {
    init((ServerConfiguration) factory);
    // Wait to trigger the out of bounds check which will call our version of getOnlineTabletsForTable
    UtilWaitThread.sleep(11000);
    Set<KeyExtent> migrations = new HashSet<KeyExtent>();
    List<TabletMigration> migrationsOut = new ArrayList<TabletMigration>();
    this.balance(createCurrent(15), migrations, migrationsOut);
    Assert.assertEquals(2, migrationsOut.size());
  }

  @Override
  public List<TabletStats> getOnlineTabletsForTable(TServerInstance tserver, String tableId) throws ThriftSecurityException, TException {
    // Report incorrect information so that balance will create an assignment
    List<TabletStats> tablets = new ArrayList<>();
    if (tableId.equals(BAR.getId()) && tserver.host().equals("192.168.0.1")) {
      // Report that we have a bar tablet on this server
      TKeyExtent tke = new TKeyExtent();
      tke.setTable(BAR.getId().getBytes());
      tke.setEndRow("11".getBytes());
      tke.setPrevEndRow("10".getBytes());
      TabletStats ts = new TabletStats();
      ts.setExtent(tke);
      tablets.add(ts);
    } else if (tableId.equals(FOO.getId()) && tserver.host().equals("192.168.0.6")) {
      // Report that we have a foo tablet on this server
      TKeyExtent tke = new TKeyExtent();
      tke.setTable(FOO.getId().getBytes());
      tke.setEndRow("1".getBytes());
      tke.setPrevEndRow("0".getBytes());
      TabletStats ts = new TabletStats();
      ts.setExtent(tke);
      tablets.add(ts);
    }
    return tablets;
  }

}
