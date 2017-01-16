Vagrant.configure(2) do |config|
   config.vm.define "bagstoreindex" do |bagstoreindex|
      bagstoreindex.vm.box = "centos/7"
      bagstoreindex.vm.hostname = "bagstoreindex"
      bagstoreindex.vm.network :private_network, ip: "192.168.33.35"
      bagstoreindex.vm.provision "ansible" do |ansible|
        ansible.playbook = "src/main/ansible/vagrant.yml"
        ansible.inventory_path = "src/main/ansible/hosts"
      end
      bagstoreindex.vm.provider "virtualbox" do |vb|
        vb.gui = false
        vb.memory = 2072
        vb.cpus = 2
        vb.customize ["guestproperty", "set", :id, "--timesync-threshold", "1000"]
        vb.customize ["guestproperty", "set", :id, "--timesync-interval", "1000"]
      end
   end
end
