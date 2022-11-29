New-AzResourceGroup -Name demo01 -Location eastasia

New-AzResourceGroupDeployment -ResourceGroupName demo01 -TemplateFile https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/storage-template.json -TemplateParameterUri https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/storage-parameters.json

New-AzResourceGroupDeployment -ResourceGroupName demo01 -TemplateFile https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/db-template.json -TemplateParameterUri https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/db-parameters.json

New-AzResourceGroupDeployment -ResourceGroupName demo01 -TemplateFile https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/asb-template.json -TemplateParameterUri https://raw.githubusercontent.com/t217145/Azure-Messaging-Solutions-Demo/main/ServiceBusDemo/ArmTemplate/asb-parameter.json