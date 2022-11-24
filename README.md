# Azure-Messaging-Solutions-Demo for Manulife

### Preparation
You need to setup in app environment and deploy corresponding azure resources

#### Local env. variables
variable name  | description
------------- | -------------
stgQueueConnStr  | The connection string to the azure storage queue
servicebus.connStr  | Azure services Bus Namespace connection string
servicebus.connStr  | Azure services Bus Namespace connection string
dbhost | Azure SQL DB Server name
dbname | Azure SQL DB name
dbusr | Login name for the Azure SQL DB
dbpwd | Login password for the Azure SQL DB

#### Azure Resources
Run following command in Azure Portal Cloud Shell

> Create Resource Group in East Asia
`New-AzResourceGroup -Name demo01 -Location eastasia`

> Create the Storage Account (beware the storage account name must have conflict)
`New-AzResourceGroupDeployment -ResourceGroupName demo01 -TemplateUri https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/storage-template.json -TemplateParameterUri https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/storage-parameters.json`

> Create the DB and DB Server (beware the DB name must have conflict)
`New-AzResourceGroupDeployment -ResourceGroupName demo01 -TemplateUri https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/db-template.json -TemplateParameterUri https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/db-parameters.json`

> Create the Azure Service Bus namespace + queue/topic + topic subscription (beware the namespace must have conflict)
`New-AzResourceGroupDeployment -ResourceGroupName demo01 -TemplateUri https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/asb-template.json -TemplateParameterUri https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/asb-parameter.json`

**Beware, you need to add the app registration and assign the service principle with Data Reader and Receiver role in ASB**
