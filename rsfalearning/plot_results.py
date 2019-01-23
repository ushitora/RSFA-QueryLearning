import seaborn as sns; sns.set()
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np


def get_results(filename):
    results = pd.read_csv(filename, header=None).iloc[:50000]
    header = [
        'i', 'target_state', 'dsfa_state', 'dsfa_mq', 'dsfa_eq', 'dsfa_eq_cached',
        'dsfa_ce_guard', 'dsfa_ce_state', 'dsfa_det_ce', 'dsfa_comp_ce',
        'rsfa_state', 'rsfa_mq', 'rsfa_eq', 'rsfa_eq_cached', 'rsfa_ce_guard',
        'rsfa_ce_table', 'rsfa_cond1_guard', 'rsfa_cond1_table', 'rsfa_cond2_guard',
        'rsfa_cond2_table', 'rsfa_cond3_guard', 'rsfa_cond3_table'
    ]
    results.columns = header
    return results


equality_results = get_results('equality_results')

sns.lineplot(x='dsfa_state', y='value', hue='variable',
    data=equality_results.melt(id_vars=['dsfa_state'], value_vars=['dsfa_eq', 'rsfa_eq']))
plt.xlim(0, 200)
plt.ylim(0, 550)
plt.xlabel('Size of minimal DFSA')
plt.ylabel('Number of asked EQs')
plt.savefig('equality_results_eq.pdf')
plt.close()

sns.lineplot(x='dsfa_state', y='value', hue='variable',
    data=equality_results.melt(id_vars=['dsfa_state'], value_vars=['dsfa_mq', 'rsfa_mq']))
plt.xlabel('Size of minimal DFSA')
plt.ylabel('Number of asked MQs')
plt.xlim(0, 200)
plt.ylim(0, 12500)
plt.savefig('equality_results_mq.pdf')
plt.close()


int_interval_results = get_results('int_interval_results')
sns.lineplot(x='dsfa_state', y='value', hue='variable',
    data=int_interval_results.melt(id_vars=['dsfa_state'], value_vars=['dsfa_eq', 'rsfa_eq']))
plt.xlim(0, 200)
plt.ylim(0, 550)
plt.xlabel('Size of minimal DFSA')
plt.ylabel('Number of asked EQs')
plt.savefig('int_interval_results_eq.pdf')
plt.close()

sns.lineplot(x='dsfa_state', y='value', hue='variable',
    data=int_interval_results.melt(id_vars=['dsfa_state'], value_vars=['dsfa_mq', 'rsfa_mq']))
plt.xlabel('Size of minimal DFSA')
plt.ylabel('Number of asked MQs')
plt.xlim(0, 200)
plt.ylim(0, 220000)
plt.savefig('int_interval_results_mq.pdf')
plt.close()


new_equality_results = get_results('new_condition3_equality_results_2_backup')
equality_results['new_condition_3_rsfa_eq'] = new_equality_results['rsfa_eq']
equality_results['new_condition_3_rsfa_mq'] = new_equality_results['rsfa_mq']

sns.lineplot(x='dsfa_state', y='value', hue='variable',
    data=equality_results.melt(id_vars=['dsfa_state'], value_vars=['dsfa_eq', 'rsfa_eq', 'new_condition_3_rsfa_eq']))
plt.xlim(0, 200)
plt.ylim(0, 550)
plt.xlabel('Size of minimal DFSA')
plt.ylabel('Number of asked EQs')
plt.savefig('new_condition3_equality_results_eq.pdf')
plt.close()

sns.lineplot(x='dsfa_state', y='value', hue='variable',
    data=equality_results.melt(id_vars=['dsfa_state'], value_vars=['dsfa_mq', 'rsfa_mq', 'new_condition_3_rsfa_mq']))
plt.xlabel('Size of minimal DFSA')
plt.ylabel('Number of asked MQs')
plt.xlim(0, 200)
plt.ylim(0, 12500)
plt.savefig('new_condition3_equality_results_mq.pdf')
plt.close()


new_int_interval_results = get_results('new_condition3_int_interval_results_backup')
int_interval_results['new_condition_3_rsfa_eq'] = new_int_interval_results['rsfa_eq']
int_interval_results['new_condition_3_rsfa_mq'] = new_int_interval_results['rsfa_mq']

sns.lineplot(x='dsfa_state', y='value', hue='variable',
    data=int_interval_results.melt(id_vars=['dsfa_state'], value_vars=['dsfa_eq', 'rsfa_eq', 'new_condition_3_rsfa_eq']))
plt.xlim(0, 200)
plt.ylim(0, 550)
plt.xlabel('Size of minimal DFSA')
plt.ylabel('Number of asked EQs')
plt.savefig('new_condition3_int_interval_results_eq.pdf')
plt.close()

sns.lineplot(x='dsfa_state', y='value', hue='variable',
    data=int_interval_results.melt(id_vars=['dsfa_state'], value_vars=['dsfa_mq', 'rsfa_mq', 'new_condition_3_rsfa_mq']))
plt.xlabel('Size of minimal DFSA')
plt.ylabel('Number of asked MQs')
plt.xlim(0, 200)
plt.ylim(0, 12500)
plt.savefig('new_condition3_int_interval_results_mq.pdf')
plt.close()
