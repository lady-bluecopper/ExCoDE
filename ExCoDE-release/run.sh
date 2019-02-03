#!/bin/bash

# Loading configurations for experiments
echo '>> Loading config file config.cfg'
source config.cfg
cat config.cfg

unset datasets
declare -A datasets
datasets[$twitter_db]=$twitter_defaults
datasets[$mobile_db]=$mobile_defaults
datasets[$haggle_db]=$haggle_defaults
datasets[$random_a_db]=$random_a_defaults
datasets[$random_b_db]=$random_b_defaults
datasets[$random_c_db]=$random_c_defaults
datasets[$random_d_db]=$random_d_defaults
datasets[$random_e_db]=$random_e_defaults
datasets[$random_f_db]=$random_f_defaults

unset test_dens
declare -A test_dens
test_dens[$twitter_db]=$twitter_dens
test_dens[$mobile_db]=$mobile_dens
test_dens[$haggle_db]=$haggle_dens
test_dens[$random_a_db]=$random_a_dens
test_dens[$random_b_db]=$random_b_dens
test_dens[$random_c_db]=$random_c_dens
test_dens[$random_d_db]=$random_d_dens
test_dens[$random_e_db]=$random_e_dens
test_dens[$random_f_db]=$random_f_dens

unset test_cor
declare -A test_cor
test_cor[$twitter_db]=$twitter_cor
test_cor[$mobile_db]=$mobile_cor
test_cor[$haggle_db]=$haggle_cor
test_cor[$random_a_db]=$random_a_cor
test_cor[$random_b_db]=$random_b_cor
test_cor[$random_c_db]=$random_c_cor
test_cor[$random_d_db]=$random_d_cor
test_cor[$random_e_db]=$random_e_cor
test_cor[$random_f_db]=$random_f_cor

unset flags
declare -A flags
flags[$twitter_db]=$twitter_flags
flags[$mobile_db]=$mobile_flags
flags[$haggle_db]=$haggle_flags
flags[$random_a_db]=$random_a_flags
flags[$random_b_db]=$random_b_flags
flags[$random_c_db]=$random_c_flags
flags[$random_d_db]=$random_d_flags
flags[$random_e_db]=$random_e_flags
flags[$random_f_db]=$random_f_flags

echo -e '\n\n>> Creating directories ...'
mkdir -p $outputFolder

for dataset in ${!datasets[@]}
do
    dataset_path="$input_data"
    default=${datasets[${dataset}]}
    flag=${flags[${dataset}]}
    # Parse default values
    defaults=(`echo $default|tr "," "\n"`)
    exps=(`echo $flag|tr "," "\n"`)

    echo ">> Processing dataset ${dataset} with default values (${defaults[@]})"
    echo ">> Experiment flags ${exps[@]}"

    if [[ ${exps[0]} -eq "1" ]]; then
        echo '-----------------------------'
        echo '      Varying Density        '
        echo '-----------------------------'

        dens=(`echo ${test_dens[${dataset}]}|tr "," "\n"`)

        if [[ $isExact == "true" ]]; then
    	    OUTPUT="${outputFolder}/exact"
            mkdir -p $OUTPUT

            for den in ${dens[*]}
            do
                echo "Running command ..."
                echo "$JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${defaults[0]} minDen=${den} isMA=true outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize isExact=$isExact maxJac=$maxJac minEdgesInSnap=${defaults[2]}"
                echo "---- `date`"

                $JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${defaults[0]} minDen=${den} isMA=true outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize isExact=$isExact maxJac=$maxJac minEdgesInSnap=${defaults[2]}

                echo "Running command ..."
                echo "$JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${defaults[0]} minDen=${den} isMA=false outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize isExact=$isExact maxJac=$maxJac minEdgesInSnap=${defaults[2]}" 
                echo "---- `date`"

                $JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${defaults[0]} minDen=${den} isMA=false outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize isExact=$isExact maxJac=$maxJac minEdgesInSnap=${defaults[2]}
            done
    	else
            OUTPUT="${outputFolder}/ax"
            mkdir -p $OUTPUT

            H=${#hashFuncs[@]}
            COUNTER=0
            while [ $COUNTER -lt $H ]; do
                for den in ${dens[*]}
                do
                    echo "Running command ..."
                    echo "$JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${defaults[0]} minDen=${den} isMA=true outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize numHashRuns=${hashRuns[${COUNTER}]} numHashFuncs=${hashFuncs[${COUNTER}]} maxJac=$maxJac isExact=$isExact minEdgesInSnap=${defaults[2]}"
                    echo "---- `date`"

                    $JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${defaults[0]} minDen=${den} isMA=true outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize numHashRuns=${hashRuns[${COUNTER}]} numHashFuncs=${hashFuncs[${COUNTER}]} maxJac=$maxJac isExact=$isExact minEdgesInSnap=${defaults[2]}

                    echo "Running command ..."
                    echo "$JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${defaults[0]} minDen=${den} isMA=false outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize numHashRuns=${hashRuns[${COUNTER}]} numHashFuncs=${hashFuncs[${COUNTER}]} maxJac=$maxJac isExact=$isExact minEdgesInSnap=${defaults[2]}"
                    echo "---- `date`"

                    $JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${defaults[0]} minDen=${den} isMA=false outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize numHashRuns=${hashRuns[${COUNTER}]} numHashFuncs=${hashFuncs[${COUNTER}]} maxJac=$maxJac isExact=$isExact minEdgesInSnap=${defaults[2]}

                done
                let COUNTER=COUNTER+1
            done
    	fi
    fi
    if [[ ${exps[2]} -eq "1" ]]; then
        echo '-----------------------------'
        echo '      Varying Correlation    '
        echo '-----------------------------'

        cors=(`echo ${test_cor[${dataset}]}|tr "," "\n"`)

        if [[ $isExact == "true" ]]; then
            OUTPUT="${outputFolder}/exact"
            mkdir -p $OUTPUT

            for cor in ${cors[*]}
            do
                echo "Running command ..."
                echo "$JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${cor} minDen=${defaults[1]} isMA=true outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize isExact=$isExact maxJac=$maxJac minEdgesInSnap=${defaults[2]}" 
                echo "---- `date`"

                $JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${cor} minDen=${defaults[1]} isMA=true outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize isExact=$isExact maxJac=$maxJac minEdgesInSnap=${defaults[2]}

                echo "Running command ..."
                echo "$JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${cor} minDen=${defaults[1]} isMA=false outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize isExact=$isExact maxJac=$maxJac minEdgesInSnap=${defaults[2]}" 
                echo "---- `date`"

                $JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${cor} minDen=${defaults[1]} isMA=false outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize isExact=$isExact maxJac=$maxJac minEdgesInSnap=${defaults[2]}
            
            done
        else
            OUTPUT="${outputFolder}/ax"
            mkdir -p $OUTPUT

            H=${#hashFuncs[@]}
            COUNTER=0
            while [ $COUNTER -lt $H ]; do
                for cor in ${cors[*]}
                do
                    echo "Running command ..."
                    echo "$JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${cor} minDen=${defaults[1]} isMA=true outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize numHashRuns=${hashRuns[${COUNTER}]} numHashFuncs=${hashFuncs[${COUNTER}]} maxJac=$maxJac isExact=$isExact minEdgesInSnap=${defaults[2]}"
                    echo "---- `date`"

                    $JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${cor} minDen=${defaults[1]} isMA=true outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize numHashRuns=${hashRuns[${COUNTER}]} numHashFuncs=${hashFuncs[${COUNTER}]} maxJac=$maxJac isExact=$isExact minEdgesInSnap=${defaults[2]}
                
                    echo "Running command ..."
                    echo "$JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${cor} minDen=${defaults[1]} isMA=false outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize numHashRuns=${hashRuns[${COUNTER}]} numHashFuncs=${hashFuncs[${COUNTER}]} maxJac=$maxJac isExact=$isExact minEdgesInSnap=${defaults[2]}"
                    echo "---- `date`"

                    $JVM $Run dataFolder=${dataFolder}/ edgeFile=${dataset}.csv isWeighted=${defaults[3]} isLabeled=${defaults[4]} minCor=${cor} minDen=${defaults[1]} isMA=false outputFolder=${OUTPUT}/ maxCCSize=$maxCCSize numHashRuns=${hashRuns[${COUNTER}]} numHashFuncs=${hashFuncs[${COUNTER}]} maxJac=$maxJac isExact=$isExact minEdgesInSnap=${defaults[2]}
                
                done
                let COUNTER=COUNTER+1
            done
        fi
    fi
done
