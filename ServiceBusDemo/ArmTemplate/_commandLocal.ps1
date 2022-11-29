New-AzResourceGroup -Name demo01 -Location eastasia

New-AzResourceGroupDeployment -ResourceGroupName demo01 -TemplateFile ./storage-template.json -TemplateParameterUri ./storage-parameters.json

New-AzResourceGroupDeployment -ResourceGroupName demo01 -TemplateFile ./db-template.json -TemplateParameterUri ./db-parameters.json

New-AzResourceGroupDeployment -ResourceGroupName demo01 -TemplateFile ./asb-template.json -TemplateParameterUri ./asb-parameter.json