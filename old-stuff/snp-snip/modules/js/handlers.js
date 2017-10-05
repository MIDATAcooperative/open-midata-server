modules['dbsnp'] = {
    position: 0,
    priority: 0, 
    handler: function($scope, dbsnpData) {

        $scope.data[$scope.rs].dbsnp = true;

        $scope.data[$scope.rs].dbsnpGeneId = dbsnpData[0];
        $scope.data[$scope.rs].dbsnpSymbol = dbsnpData[1];

    }
};

modules['snpedia'] = {
    position: 1,
    priority: 1, 
    handler: function($scope, snpediaData) {

        $scope.data[$scope.rs].snpedia = true;

        $scope.data[$scope.rs].snpediaText = $scope.angular['sce'].trustAsHtml(snpediaData[0]);
        $scope.data[$scope.rs].snpediaOrientation = snpediaData[1]; 

    }
};

modules['hapmap'] = {
    position: 2,
    priority: 0, 
    handler: function($scope, hapmapData) {

        $scope.data[$scope.rs].hapmap = true;

        var url = "http://chart.apis.google.com/chart?cht=bhs&chd=t:"+hapmapData[4]+","+hapmapData[8]+","+hapmapData[12]+","+hapmapData[16]+","+hapmapData[20]+","+hapmapData[24]+","+hapmapData[28]+","+hapmapData[32]+","+hapmapData[36]+","+hapmapData[40]+","+hapmapData[44]+"|"+hapmapData[5]+","+hapmapData[9]+","+hapmapData[13]+","+hapmapData[17]+","+hapmapData[21]+","+hapmapData[25]+","+hapmapData[29]+","+hapmapData[33]+","+hapmapData[37]+","+hapmapData[41]+","+hapmapData[45]+"|"+hapmapData[6]+","+hapmapData[10]+","+hapmapData[14]+","+hapmapData[18]+","+hapmapData[22]+","+hapmapData[26]+","+hapmapData[30]+","+hapmapData[34]+","+hapmapData[38]+","+hapmapData[42]+","+hapmapData[46]+"&chs=275x200&chbh=8,5&chxl=0:|1:|"+hapmapData[43]+"|"+hapmapData[39]+"|"+hapmapData[35]+"|"+hapmapData[31]+"|"+hapmapData[27]+"|"+hapmapData[23]+"|"+hapmapData[19]+"|"+hapmapData[15]+"|"+hapmapData[11]+"|"+hapmapData[7]+"|"+hapmapData[3]+"||&chxt=x,y&chco=CD853F,30FF30,0000FF,FF00FF&chls=1,1,0|1,1,0|1,1,0|1,1,0";
        var html = '<table><tbody><tr><th class="text-center"><span style="font-size:1.25em"><span style="color:#CD853F">('+($scope.data[$scope.rs].snpediaOrientation === "plus" ? hapmapData[0] : $scope.changeOrientation(hapmapData[0]))+')</span><span style="color:#20D020">('+($scope.data[$scope.rs].snpediaOrientation === "plus" ? hapmapData[1] : $scope.changeOrientation(hapmapData[1]))+')</span><span style="color:#0000FF">('+($scope.data[$scope.rs].snpediaOrientation === "plus" ? hapmapData[2] : $scope.changeOrientation(hapmapData[2]))+')</span></span> </th></tr><tr><td colspan="3"><img src="'+url+'"></td></tr></tbody></table>';

        $scope.data[$scope.rs].hapmapChart = $scope.angular['sce'].trustAsHtml(html);

    }
};
