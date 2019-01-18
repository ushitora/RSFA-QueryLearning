import seaborn as sns; sns.set()
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

results = pd.read_csv('random_nfa_experiment_tail_removed.txt', header=None).iloc[:50000]
header = [
    'i', 'target_state', 'dsfa_state', 'dsfa_mq', 'dsfa_eq', 'dsfa_eq_cached',
    'dsfa_ce_guard', 'dsfa_ce_state', 'dsfa_det_ce', 'dsfa_comp_ce',
    'rsfa_state', 'rsfa_mq', 'rsfa_eq', 'rsfa_eq_cached', 'rsfa_ce_guard',
    'rsfa_ce_table', 'rsfa_cond1_guard', 'rsfa_cond1_table', 'rsfa_cond2_guard',
    'rsfa_cond2_table', 'rsfa_cond3_guard', 'rsfa_cond3_table'
]
results.columns = header

sns.lineplot(x='dsfa_state', y='value', hue='variable', data=results.melt(id_vars=['dsfa_state'], value_vars=['dsfa_eq', 'rsfa_eq']))
plt.xlim(0, 200)
plt.ylim(0, 600)
plt.xlabel('Size of minimal DFSA')
plt.xlabel('Number of asked EQs')
plt.savefig('eq.pdf')

sns.lineplot(x='dsfa_state', y='value', hue='variable', data=results.melt(id_vars=['dsfa_state'], value_vars=['dsfa_mq', 'rsfa_mq']))
plt.xlabel('Size of minimal DFSA')
plt.xlabel('Number of asked MQs')
plt.xlim(0, 200)
plt.savefig('mq.pdf')
