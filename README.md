# timestamp
Code release for Paper 'Cleaning Timestamps with Temporal Constraints'

## Dataset
- climatology data: https://cloud.tsinghua.edu.cn/f/645c205eac434f479900/
- biomedical science data: https://cloud.tsinghua.edu.cn/f/bd444fbc5472479894bc/
- smart cities data: https://cloud.tsinghua.edu.cn/f/ca725efc5cc04080a14c/
- smart home data: https://cloud.tsinghua.edu.cn/f/1e79cd407d6b4bfdb955/
- IoT data: https://cloud.tsinghua.edu.cn/f/40013e31a5734a5b8e0d/
- semi-synthetic data: https://cloud.tsinghua.edu.cn/f/aa51aabf0bf741ac8bce/
- walking data: https://cloud.tsinghua.edu.cn/f/8f2d1d64c7a0404fac15/


## Instruction
All experiments can be conducted by calling the static class ExpMain and StreamExpMain.

### ExpMain.java
The parameters of static function expMain are listed as follows
- truth: ground truth data
- fault: data with error
- constraintList: list of all temporal constraints
- minimumNetwork: corresponding minimum network
- chainMaxLength: maximum length of provenance chain when generating candidates
- pruneFlag: whether to use prune
- indexFlag: whether to use index
- indexBudget: budget of index
- accuracyAcc: array to store accuracy
- timeAcc: array to store time cost

### StreamExpMain.java
The parameters of static function expMain are listed as follows
- truth: ground truth data
- fault: data with error
- order: order of the data node
- constraintList: list of all temporal constraints
- minimumNetwork: corresponding minimum network
- windowSize: size of sliding window
- stepSize: step size when sliding to the next window
- chainMaxLength: maximum length of provenance chain when generating candidates
- accuracyAcc: an array to store accuracy
- timeAcc: an array to store time cost

## Experiment
### Fig 8
By calling function ExpMain.expMain with different settings of parameter chainMaxLength

### Fig 9
By calling function ExpMain.expMain with different settings of parameters pruneFlag and indexFlag

### Fig 10 & 11
By calling function ExpMain.expMain with different settings of parameter indexBudget

### Fig 13 - 20
By calling function ExpMain.expMain with different settings of parameters truth, fault, constraintList and minimumNetwork

### Fig 21
By calling function StreamExpMain.expMain with different settings of parameter windowSize

### Fig 22
By calling function StreamExpMain.expMain with different settings of parameter windowSize

### Fig 23
By calling function ExpMain.expMain with different settings of parameters truth, fault, constraintList and minimumNetwork

### Fig 24
By calling function StreamExpMain.expMain with different settings of parameter windowSize
