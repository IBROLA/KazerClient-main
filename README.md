# Kazer 缝合抄袭客户端源码泄露



> **"我只在网上看到过发布 release 不小心把源码打包进去的，现在我真的见到了。"\*\*  

> —— *Leaked by IBROLA*



---



## 事件速览

嚣张的在Bilibili平台发布对Lunar客户端踩一捧一的行为, 并且还使用的是来自CheatBreaker/Lunar团队的客户端代码
视频发布者UID: 1387502519
视频链接: https://www.bilibili.com/video/BV1LQpm6pExQ

<img width="1214" height="631" alt="image" src="https://github.com/user-attachments/assets/569fb05a-89c2-4dc8-8849-6fda93002b09" />


本项目记录并归档了 **Kazer 客户端** 在发布正式 Release 时，因打包构建脚本配置失误，直接将完整未混淆源代码（Source Code）一并压入发行包的“重大事故现场”。



通过对该包内源码的初步审计，确认该客户端存在大量对其他开源/非开源客户端源码的原封不动复制粘贴与无意义缝合行为。



---



##  泄露详情



- **泄露方式**：官方 Release 压缩包/构建产物内未排除源码目录（`src/` 或源码文件直接混入构建产物）。

- **泄露来源**：Kazer 官方公开 Release 构建

- **泄露归档**：IBROLA

- **源码状态**：完整未混淆 / 保留原始注释与包结构



---



## 🤡 典型“缝合”特征赏析



翻阅该泄露源码，可以清晰观察到以下几大经典 Skidder（缝合怪）特征：



1. **原封不动的受害者客户端**：虽然声称“KazerClient”，但超过96%源码都来自CheatBreaker/Lunar团队。

2. **包名掩耳盗铃**：用AI Vibe coding几个类其他啥也没改就改名叫KazerClient, 在assets里甚至没删干净GPT工作留下的文件。

3. **惊世打包工艺**：构建脚本直接把全量源码打包成最终分发包，做到了真正的“开源透明”。

