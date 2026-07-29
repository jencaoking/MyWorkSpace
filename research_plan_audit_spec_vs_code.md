# 审计计划：自律工作台 方案 V1.1 vs 实际代码

## 目标
对照 `自律工作台_完整方案_V1.1.md`，核查当前代码实现，输出：
- 已完成（Done）
- 未实际完成（Not done / 仅占位）
- 与方案不符（Mismatch，如架构、技术选型、字段偏差）

## 拆分任务（并行子代理）
1. **Android 功能模块审计**（code-explorer）
   - 覆盖范围：task、notes、sport、english、media、health、account(ledger)、pomodoro、tools、weather
   - 对照：功能矩阵 2.1、页面清单 11.1、阶段2~6 里程碑
   - 关注：任务统计/日历、运动步数、影音TMDB搜索、英语翻译/录音、健康复诊提醒、记账MPAndroidChart统计、天气

2. **Android 基础设施审计**（code-explorer）
   - 对照：第四章架构(4.1~4.3)、第八章同步、第九章通知、第十二章主题、十一章导航/板块开关、全局搜索(FTS)
   - 关注：是否用 Compose 而非方案所述 ViewBinding/Fragment；主题深浅色切换；板块开关；FTS搜索；AlarmManager/WorkManager/同步；DataStore deviceId；Retrofit 后端集成

3. **PHP 后端审计**（code-explorer）
   - 对照：第五章结构、第六章 schema、第七章接口、附录D 目录
   - 关注：目录结构、Controllers、路由、/sync 接口、第三方代理、schema.sql、Auth deviceId；与方案偏差

## 综合
主代理汇总三方结果，按"已完成/未实际完成/不符"三类输出报告，标注证据文件。
