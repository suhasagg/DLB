package org.cloudbus.cloudsim.myTest;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DLB.DLBCore;
//import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.ConsistentHashingWithBoundedLoad.ConsistentHashingWithBoundedLoad;
import org.cloudbus.cloudsim.ConsistentHashingWithBoundedLoad.ConsistentHashRouter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.distributions.LognormalDistr;
import org.cloudbus.cloudsim.util.ModelingDataDistribution;
import org.cloudbus.cloudsim.distributions.*;

public class TestDLB {
	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmList;
	private static List<ConsistentHashingWithBoundedLoad> nodeList;
	private static ConsistentHashRouter<ConsistentHashingWithBoundedLoad> consistentHashRouter;

	public static void main(String[] args) {
		int numUser = 1;
		int cloudletNum = 8692;
		int vmNum = 256;
		Calendar calendar = Calendar.getInstance();
		boolean trace_flag = false;

		CloudSim.init(numUser, calendar, trace_flag);

		Datacenter datacenter = createDatacenter("dataCenter:0");

		DatacenterBroker broker = createBroker();
		int brokerId = broker.getId();

		vmList = new ArrayList<Vm>();
		ModelingVmList(vmList, brokerId, vmNum, 256);

		broker.submitVmList(vmList);
		cloudletList = new ArrayList<Cloudlet>();

		int cloudletLength = 20;
		int cloudletFileSize = 1;
		long cloudletOutputSize = 1;
		//UtilizationModel utilizationModel = new UtilizationModelFull();
		//DLBCore _dlbCore = new DLBCore("/home/zhuxk/eclipse-workspace/models/Model4DLB", "frozen_model_normal.pb");
		//DLBCore _dlbCore = new DLBCore("/home/zhuxk/eclipse-workspace/models/Model4DLB", "frozen_model_lognormal.pb");
		DLBCore _dlbCore = new DLBCore("/home/zhuxk/eclipse-workspace/models/Model4DLB", "frozen_model_lognormal.pb");
		//_dlbCore.GetPos(10);
		CloudletMap(cloudletNum, vmList, cloudletLength, cloudletFileSize, cloudletOutputSize, 1, brokerId, _dlbCore);

		broker.submitCloudletList(cloudletList);
		CloudSim.startSimulation();

		CloudSim.stopSimulation();

		List<Cloudlet> newList = broker.getCloudletReceivedList();

		printCloudletList(newList);
		//printVmList(vmList);
	}

	private static Datacenter createDatacenter(String name) {
		List<Host> hostList = new ArrayList<Host>();
		List<Pe> peList = new ArrayList<Pe>();
		int mips = 256;
		peList.add(new Pe(0, new PeProvisionerSimple(mips)));

		int hostId = 0;
		int ram = 1024 * 256;// 1024MB
		long storage = 1024 * 10 * 256; //
		int bw = 1024 * 256;

		hostList.add(new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList,
				new VmSchedulerTimeShared(peList)));
		String arch = "x64";
		String os = "Linus";
		String vmm = "Xen";
		double timeZone = 10.0;
		double cost = 109.0;
		double costPerMem = 1;
		double costPerStorage = 1;
		double costPerBw = 1000;
		LinkedList<Storage> storageList = new LinkedList<Storage>();
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, timeZone,
				cost, costPerMem, costPerStorage, costPerBw);
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return datacenter;
	}

	private static Cloudlet CreateCloudlet(int cloudletId, int cloudletLength, int cloudletFileSize,
			long cloudletOutputSize, int pesNumber, int brokerId, int vmId) {
		UtilizationModel utilizationModel = new UtilizationModelFull();
		Cloudlet cloudlet = new Cloudlet(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize,
				utilizationModel, utilizationModel, utilizationModel);
		cloudlet.setUserId(brokerId);
		cloudlet.setVmId(vmId);
		Cloudlet cloudlet1 = new Cloudlet(1, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize,
				utilizationModel, utilizationModel, utilizationModel);
		cloudlet1.setUserId(brokerId);
		cloudlet1.setVmId(vmId);
		return cloudlet;
	}

	private static void CloudletMap(int cloudletNum, List<Vm> vmList, int cloudletLength, int cloudletFileSize,
			long cloudletOutputSize, int pesNumber, int brokerId, DLBCore _dlbCore) {
		Object obj = new Object();
		
		int vmId;
	    Set<Integer> hashset=CreateHashset(cloudletNum);
	    HashSet<Integer> a = new HashSet<Integer>();
		//a = ModelingDataDistribution.DataGeneratorUniform( 20000, 0, cloudletNum);
		//a = ModelingDataDistribution.DataGeneratorNormal( 40000, 92, cloudletNum);
		a = ModelingDataDistribution.DataGeneratorLognormal( 10, 0.1, cloudletNum);
		//a = ModelingDataDistribution.DataGeneratorExp( 3000, cloudletNum);
		Iterator<Integer> iterator = a.iterator();

		double logits;
		int cloudletId;
		while(iterator.hasNext()) {
			iterator.toString();
			cloudletId = iterator.next().intValue();
			logits = (double)cloudletId;
	        vmId = _dlbCore.GetPos(logits)%256;
			Cloudlet cloudlet = CreateCloudlet(cloudletId , cloudletLength, cloudletFileSize, cloudletOutputSize,
					pesNumber, brokerId, vmId);
			cloudletList.add(cloudlet);
		}
	}
	private static void CloudletMap(int cloudletNum, List<Vm> vmList, int cloudletLength, int cloudletFileSize,
			long cloudletOutputSize, int pesNumber, int brokerId) {
		Object obj = new Object();
		int cloudletId = 0;
		int vmId;

		for (int i = 0; i < cloudletNum; i++) {
			vmId = ConsistentHashingWithBoundedLoad.GetBin(consistentHashRouter, i + "");

			System.out.printf("vmId:%d\n", vmId);

			System.out.printf("vmID:%d,vmList.size():%d\n", vmId, vmList.size());
			Cloudlet cloudlet = CreateCloudlet(cloudletId = i, cloudletLength, cloudletFileSize, cloudletOutputSize,
					pesNumber, brokerId, vmId);
			cloudletList.add(cloudlet);
		}
		return;
	}

	private static void ModelingVmList(List<Vm> vmList, int brokerId, int vmNum, int boundedLoad) {
		int mips = 1;
		long size = 1024 * 10;
		int ram = 1024;
		long bw = 1024;
		int pesNumber = 1;
		String vmm = "Xen";
		// vmList = new ArrayList<Vm>();
		nodeList = new ArrayList<ConsistentHashingWithBoundedLoad>();
		for (int i = 0; i < vmNum; i++) {
			Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
			nodeList.add(new ConsistentHashingWithBoundedLoad("Bin", "127.0.0.2", i, boundedLoad));
			vmList.add(vm);
		}
		consistentHashRouter = new ConsistentHashRouter<>(nodeList, 100);// 10 virtual node
		return;
	}

	private static DatacenterBroker createBroker() {
		DatacenterBroker broker = null;

		try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return broker;
	}

	private static void printVmList(List<Vm> list) {
		int size = list.size();
		
		String indent = "    ";
		Vm vm;
		Log.printLine();
		try {
			File f = new File("/home/zhuxk/eclipse-workspace/DLB/outputs/vmResults.csv");
			if (!f.exists()) {
				f.createNewFile();
			}
			Log.printLine("========== VM-OUTPUT ==========");
			Log.printLine("vm ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent);
			for (int i = 0; i < size; i++) {
					vm = list.get(i);
					Log.printLine(
							indent + vm.getUid() +
							indent + vm.getCurrentRequestedBw() +
							indent + vm.getMips() +
							indent + vm.getTotalUtilizationOfCpu(11) +
							indent + vm.isInMigration()
					
					);
				}
			
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		try {
			File f = new File("/home/zhuxk/eclipse-workspace/DLB/outputs/results.csv");
			if (!f.exists()) {
				f.createNewFile();
			}
			FileOutputStream fop = null;

			fop = new FileOutputStream(f);

			Log.printLine("========== cloudlet - OUTPUT ==========");
			Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent
					+ "Time" + indent + "Start Time" + indent + "Finish Time");
			fop.write(
					new String("Cloudlet ID" + "," + "STATUS" + "," + "Data center ID" + "," + "VM ID" + ","
					+ "Time" + "," + "Start Time" + "," + "Finish Time").getBytes()
					);

			DecimalFormat dft = new DecimalFormat("###.##");
			for (int i = 0; i < size; i++) {
				cloudlet = list.get(i);
				Log.print(indent + cloudlet.getCloudletId() + indent + indent);

				if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
					Log.print("SUCCESS");
					Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent
							+ cloudlet.getVmId() + indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent
							+ indent + dft.format(cloudlet.getExecStartTime()) + indent + indent
							+ dft.format(cloudlet.getFinishTime()) 
							//+ indent + cloudlet.getCloudletFinishedSoFar()
							//+ indent + cloudlet.getCostPerSec()
							//+ indent + cloudlet.getActualCPUTime()
							//+ indent + cloudlet.getProcessingCost()
							//+ indent + cloudlet.getWallClockTime()
							//+ indent + cloudlet.getCostPerSec()
							//+ indent + cloudlet.getActualCPUTime(2)
							);
					fop.write("\n".getBytes());
					fop.write(
							new String(cloudlet.getCloudletId() + "," + "SUCCESS" + ","+
									cloudlet.getResourceId() + "," + 
									cloudlet.getVmId() + "," + 
									dft.format(cloudlet.getActualCPUTime()) + "," + 
									dft.format(cloudlet.getExecStartTime()) + "," + 
									dft.format(cloudlet.getFinishTime())).getBytes()
							);
				}			
			}
			fop.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static Set<Integer> CreateHashset(int size){	    
		Set<Integer> hashset=new HashSet();
		for(int i=0;;i++){
	        hashset.add(1+(int)(Math.random()*20000)%20000);
	        //如果容量等于cloudletNum  跳出循环, 最大不超过20000
	        if(hashset.size()==size){
	            break;
	        }
	    }
		return hashset;
	}
}
