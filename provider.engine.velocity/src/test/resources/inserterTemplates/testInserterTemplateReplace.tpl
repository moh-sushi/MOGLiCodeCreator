@TargetFileName testInserterTargetFile.txt # Name of file with extension without path
@TargetDir temp
@ReplaceStart start
@ReplaceEnd end

#foreach($classDescriptor in $model.classDescriptorList)

'	${classDescriptor.simpleName}

#end
