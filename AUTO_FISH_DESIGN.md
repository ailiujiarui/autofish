# Auto Fish 26.1.2 设计方案

## 目标

制作一个面向 Minecraft 26.1.2 的 NeoForge 客户端 Mod，提供自动钓鱼功能和游戏内配置界面，并原生兼容 Aquaculture 2（水产 2）。功能只调用客户端正常的鱼竿使用动作，不修改服务端逻辑、不伪造数据包、不绕过反作弊。

## 参考范围

本项目是 GPL-3.0 许可下对 `troyhayes/autofish` 的 Minecraft 26.1.2 移植。保留上游的单人服务端计时、多人物理包/声音检测、动作调度和高级配置，并按 26.1.2 的 Mojang 映射与渲染 API 更新实现。

## 第一版功能（MVP）

1. `F8` 切换自动钓鱼，`F9` 打开游戏内配置页。
2. 单人世界从服务端 `FishingHook.catchingFish` 获取 `nibble`；多人服务器监听鱼漂速度包或附近溅水声音。
3. 咬钩后自动收竿，等待可配置延迟，再次抛竿，并防止重复收竿。
4. 支持持久模式、多鱼竿热键栏切换、防损坏和 ClearLag 消息正则重抛。
5. 玩家移动、切换物品、打开非 ESC 暂停菜单的界面、失去焦点或不满足潜行条件时暂停动作；ESC 菜单是否继续由独立配置控制。
6. 在客户端 HUD 显示关闭、等待、收竿、重抛和暂停原因。
7. 原生配置界面和版本化 JSON 配置不依赖 Cloth Config、YACL 或 Mod Menu。

## 配置项

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| 自动钓鱼 | 开启 | 总开关，可由按键切换 |
| 多鱼竿切换 | 关闭 | 收竿后选择热键栏第一个可用鱼竿 |
| 防止损坏 | 关闭 | 不使用只剩一次耐久的鱼竿重抛 |
| 持久模式 | 关闭 | 鱼漂缺失且没有重抛任务时每 10 秒检查补抛 |
| 声音检测 | 关闭 | 多人检测使用溅水声音；关闭时使用速度包 |
| 强制多人检测 | 关闭 | 单人世界也使用多人检测路径，便于兼容与测试 |
| 收竿后延迟 | 1500 ms | 收竿到重新抛竿的等待时间，范围 500-5000 ms |
| ClearLag 正则 | 上游默认值 | 匹配实体清理聊天消息并安排重抛 |
| 显示状态 HUD | 开启 | 控制状态提示显示 |
| 仅在潜行时运行 | 关闭 | 便于和其他场景共存 |
| ESC 菜单继续钓鱼 | 开启 | 自动钓鱼流程活跃时，打开暂停菜单仍继续世界 tick、收竿与重抛 |

配置界面使用 NeoForge 客户端事件注册，并采用 26.1.2 原生 `Screen` 实现，因此不依赖 YACL。配置文件使用 JSON，版本字段用于后续迁移。

## 状态机

```text
OFF
  -> READY（启用且满足运行条件）
READY
  -> WAITING_BITE（鱼漂存在）
  -> CASTING（执行抛竿）
WAITING_BITE
  -> REELING（检测到咬钩）
  -> PAUSED（运行条件失效）
REELING
  -> DELAY（收竿成功）
DELAY
  -> CASTING（延迟结束且条件满足）
PAUSED
  -> READY（条件恢复）
```

所有收竿、切换和重抛动作都回到客户端线程并通过队列调度；`RECAST` 队列状态用于阻止同一次咬钩重复收竿。实现使用 Minecraft 26.1.2 的 Mojang 映射。

## 兼容与安全边界

- 目标平台：Minecraft 26.1.2、NeoForge 26.1.x，客户端 Mod，Java 25。
- 默认不在服务器连接期间强制运行；用户可手动关闭。
- 不提供自动整理背包、自动售卖、穿透检测或任何服务端自动化功能。
- 失去焦点、死亡、维度切换和网络断开时立即停止动作；暂停菜单仅在 `runWhilePaused` 开启且钓鱼流程活跃时放行。
- 多人检测只读取客户端正常收到的声音、速度和系统聊天包，不修改或伪造网络包。

## 工程结构预期

- `client/AutoFishClient`：客户端入口、按键和 tick 注册。
- `client/FishingController`：状态机与动作节流。
- `client/monitor`：多人速度和声音检测器。
- `client/scheduler`：收竿后的鱼竿切换与重抛任务。
- `client/mixin`：单人鱼漂逻辑与多人客户端包回调。
- `client/AutoFishConfig`：配置模型、JSON 读写和默认值。
- `client/AutoFishConfigScreen`：配置界面。
- HUD 通过 NeoForge 客户端 GUI Overlay 事件注册。

## 验证计划

1. 在干净的 26.1.2 客户端启动并打开配置界面。
2. 单人世界验证服务端咬钩回调、收竿、延迟重抛和暂停条件。
3. 验证重新加载游戏后配置保持不变。
4. 在无鱼竿、低耐久、非水域、打开容器和切换物品时确认不会误操作。
5. 对配置约束和动作调度编写单元测试；对单双人检测流程做运行测试。

## 1.0.1 配置界面崩溃修复设计

### 现象与根因

在世界内按 `F9` 打开配置界面时，客户端抛出 `IllegalStateException: Can only blur once per frame`。Minecraft 26.1.2 的最终渲染入口 `Screen.extractRenderStateWithTooltipAndSubtitles` 会自动调用一次 `extractBackground`；当前 `AutoFishConfigScreen.extractRenderState` 又手动调用一次，导致同一帧重复注册背景模糊层。

### 修复方案

- 删除 `AutoFishConfigScreen.extractRenderState` 中手动调用的 `extractBackground`。
- 保留 `super.extractRenderState` 负责提取按钮等控件，并继续绘制页面标题。
- 不禁用原版背景模糊效果，不引入 Mixin 或兼容分支。

### 回归验证

1. 从世界内按 `F9` 打开配置页，确认不再崩溃且背景只模糊一次。
2. 修改选项并点击完成，确认配置保存；点击取消或按 Esc，确认不保存。
3. 连续打开和关闭配置页，确认没有渲染异常。
4. 执行 `clean build` 和现有单元测试，检查发布 JAR。

日志中的原版资源对象缺失和 Mojang/Realms 401 与本次崩溃无关，应由启动器补全资源并刷新登录凭据解决，不纳入 Mod 代码修改。

## 1.1 上游功能移植设计

### 调整原因

`troyhayes/autofish` 并非只读取客户端鱼漂字段。它针对不同环境使用两套检测机制：单人世界从服务端鱼漂逻辑读取可捕获计时，多人服务器监听鱼漂附近的溅水声音或鱼漂速度更新包。当前实现只读取客户端 `FishingHook.nibble`，在多人服务器上该服务端计时状态通常不会同步，因此不能作为完整的上游高版本替代品。

### 移植范围

- 保留上游的单人/多人检测分流。
- 单人世界：向 26.1.2 的 `FishingHook` 钓鱼逻辑注入回调，读取 `nibble`，再切回客户端线程收竿。
- 多人服务器默认使用鱼漂速度包检测，并提供溅水声音检测选项。
- 移植重抛调度、重复收竿保护、持久模式、多鱼竿切换、防损坏和 ClearLag 聊天正则重抛。
- 按 26.1.2 的 Mojang 映射、网络包处理方法、物品栏 API 和 Java 25 重写所有版本接口，不保留 1.19.3 的 Yarn 名称。
- 配置界面继续使用当前原生 `Screen`，不强制引入 Cloth Config 或 Mod Menu；同时修复 1.0.1 的重复背景模糊问题。

### 许可证

上游采用 GPL-3.0。若直接移植或改写其代码结构，本项目也必须从 MIT 改为 GPL-3.0，并保留上游作者 `troyhayes` 的版权与来源说明。发布源码和衍生版本时继续遵守 GPL-3.0。

### 验证重点

1. 单人世界验证服务端 `nibble` 回调只触发一次收竿。
2. NeoForge 多人服务器分别验证速度检测与声音检测。
3. 验证持久模式、多鱼竿、防损坏和 ClearLag 重抛。
4. 验证断线、切换世界和关闭功能时会清空待执行动作。
5. 验证配置页不再重复模糊，并检查配置迁移和 GPL 发布元数据。

## 已确认事项

- NeoForge 是唯一目标加载器；原 Fabric 发布目标由本节之后的 1.2 设计取代。
- 配置界面使用内置原生界面，不引入 YACL。
- HUD 默认显示在左上角；第一版暂不提供位置自定义。

## 1.2 Aquaculture 2（水产 2）兼容设计

### 现状核对

截至 2026-07-27，Aquaculture 2 主分支面向 Minecraft `26.1.x`，其依赖范围为 `[26.1,26.2)`，因此源码层面覆盖 26.1.2。它当前是 NeoForge Mod；Auto Fish 同步迁移为原生 NeoForge 客户端 Mod，不使用 Fabric 兼容层。

Aquaculture 2 的四根鱼竿 `iron_fishing_rod`、`gold_fishing_rod`、`diamond_fishing_rod`、`neptunium_fishing_rod` 均使用 `AquaFishingRodItem`，该类继承原版 `FishingRodItem`，所以当前 `instanceof FishingRodItem` 已能覆盖手持判断、多鱼竿切换和防损坏判断。Aquaculture 同时把这些鱼竿加入通用标签 `c:tools/fishing_rod`。

Aquaculture 的 `AquaFishingHookEntity` 继承原版 `FishingHook` 并写入 `player.fishing`，因此现有多人速度包/声音检测可以复用。但是它覆写了 `catchingFish(BlockPos)`；当前只注入原版 `FishingHook.catchingFish` 的单人检测不会命中该覆写方法，水中和熔岩钓鱼都需要显式补充回调。

### 兼容实现边界

1. 鱼竿识别使用“`FishingRodItem` 子类或 `c:tools/fishing_rod` 标签”的联合规则。这样既覆盖当前 Aquaculture 实现，也允许其以后调整继承结构，并可复用于其他遵循通用标签的鱼竿 Mod。
2. Aquaculture 作为编译期可选依赖接入，但不设为运行时强制依赖；Aquaculture 未安装时，原版和其他鱼竿行为保持不变。
3. 在 NeoForge 服务端实体 tick 后事件中统一处理所有 `FishingHook` 实例，并通过原版 `FishingHook` Accessor Mixin 读取 `nibble`。事件发生在具体实体自身 tick 完成后，因此既覆盖原版 `catchingFish`，也覆盖 Aquaculture 浮漂覆写的 `catchingFish`（包括熔岩钓鱼），无需引用 Aquaculture 类或增加可选类 Mixin。
4. Aquaculture 水中与熔岩咬钩均走上述覆写方法，因此共用同一检测入口；收竿与重抛仍使用正常的客户端物品交互，不调用 Aquaculture 私有 API。
5. 多人环境继续默认使用速度包或溅水声音检测，不依赖服务端安装 Auto Fish。Aquaculture 的非原版熔岩声音不能作为唯一检测源，速度包和单人服务端回调作为主要路径。
6. 发布物为原生 NeoForge JAR，可与同版本 Aquaculture 2 直接同装；不再发布 Fabric JAR，避免两个加载器产物混淆。

### 验证计划

1. 单元测试覆盖原版鱼竿子类识别、`c:tools/fishing_rod` 标签后备识别，以及无标签普通物品拒绝。
2. 无 Aquaculture 环境执行 `clean build` 和现有全部测试，确认可选兼容逻辑不造成类加载失败。
3. 在 Minecraft 26.1.2、Aquaculture 2 26.1.x 与选定加载方案的实际客户端中，分别验证铁、金、钻石和海王锭鱼竿的手持识别、自动收竿、延迟重抛、多鱼竿切换和防损坏。
4. 单人世界分别验证水中钓鱼和带熔岩钩的熔岩钓鱼，确认一次咬钩只触发一次收竿。
5. 多人服务器分别验证速度检测与声音检测，并确认 Aquaculture 自定义浮漂实体仍与 `player.fishing` 和运动包 ID 正确关联。
6. 完成实现后对可选 Mixin、加载器兼容性、重复触发保护和无模组回归进行完整 Code Review，再生成发布 JAR；不自动推送或部署。

### 已确认决策

- 目标组合为 Minecraft 26.1.2、NeoForge 26.1.x 和 Aquaculture 2 26.1.x。
- Auto Fish 改为原生 NeoForge 客户端 Mod，不依赖 Fabric 兼容层，也不继续维护 Fabric 构建。

## 1.3 ESC 暂停菜单继续钓鱼设计

### 行为目标

玩家在单人世界按 ESC 后，自动钓鱼仍能等待咬钩、收竿并按配置延迟重抛；多人服务器打开 ESC 菜单时同样继续客户端自动操作。该功能通过配置项“ESC 菜单继续钓鱼”控制，默认开启。

### 实现方案

1. 配置版本升级，增加 `runWhilePaused` 布尔项并加入原生配置界面与中英文文本；旧配置迁移时采用默认开启。
2. `FishingController.unavailableReason` 只对白名单中的原版 `PauseScreen` 放行。箱子、聊天、Auto Fish 配置页及其他界面仍返回“界面已打开”，避免在交互界面中误用鱼竿。
3. 向声明该方法的 `Screen.isPauseScreen()` 增加客户端 Mixin，并在运行时严格限定 `this instanceof PauseScreen`。Minecraft 26.1.2 的 `PauseScreen` 继承该方法但不自行声明，直接以子类为 Mixin 目标会因找不到注入点而启动失败。仅当下列条件全部满足时返回 `false`，从而让单人整合服务器继续 tick：
   - Auto Fish 和 `runWhilePaused` 均已开启；
   - 玩家仍在世界中、存活并手持原版、Aquaculture 2 或通用标签鱼竿；
   - 当前存在鱼漂、已有待执行的重抛任务，或持久模式已开启。
4. 收竿后鱼漂会短暂消失，因此“待重抛任务”必须继续维持非暂停状态，直到延迟任务执行或取消，避免服务器在收竿与重抛之间停住。
5. 当玩家关闭 Auto Fish、切走鱼竿、任务结束且没有鱼漂，或关闭该配置项时，暂停菜单立即恢复原版单人暂停语义。
6. 不修改其他 `Screen` 的暂停行为，不让配置界面或容器界面后台执行自动操作。

### 验证计划

1. 单人世界用原版鱼竿抛竿后按 ESC，确认鱼漂计时继续、咬钩后自动收竿并重抛。
2. 单人世界用 Aquaculture 2 四类鱼竿以及熔岩钩重复验证，并确认延迟阶段不会重新暂停服务器。
3. 关闭“ESC 菜单继续钓鱼”后按 ESC，确认世界和自动钓鱼恢复原版暂停行为。
4. 打开箱子、聊天和 Auto Fish 配置页，确认自动操作仍暂停。
5. 在无鱼漂且未启用持久模式时按 ESC，确认不会无条件破坏单人暂停。
6. 多人环境打开 ESC 菜单，确认客户端自动收竿与重抛继续且不产生重复动作。
7. 执行完整测试、NeoForge 单独启动、与 Aquaculture 2 联合启动及代码审查后再生成发布 JAR。

## 1.4 NeoForge 与 Fabric 分支并行设计

### 分支策略

当前目录不是 Git 仓库。实施多加载器并行前，先以已经完成验证的 NeoForge 1.2.0 工程初始化本地 Git，并建立两个长期分支：

- `neoforge-26.1.2`：保留当前 NeoForge 26.1.2.87 实现，作为 NeoForge 发布与后续修复分支。
- `fabric-26.1.2`：从同一基线分出，替换加载器入口、事件注册、配置路径和发布元数据，生成 Fabric 专用 JAR。

两个分支不共用构建目录或发布 JAR。通用状态机、配置模型、原生配置界面、动作调度、网络包检测和三个 Mixin 尽量保持相同；通用修复通过明确的 cherry-pick 在两个分支间同步，不直接复制整个目录覆盖另一分支。

### Fabric 适配边界

1. 使用 Fabric Loom、Fabric Loader 与适配 Minecraft 26.1.2 的 Fabric API，继续采用 Mojang 官方映射和 Java 25。
2. NeoForge `@Mod` 入口改为 Fabric `ClientModInitializer`；客户端 tick、HUD、按键和断线清理由 Fabric API 对应事件注册。
3. NeoForge 服务端实体 tick 后事件不可直接复用。Fabric 单人检测改为向 `FishingHook.tick()` 尾部注入客户端环境 Mixin，读取服务端实例的 `nibble` 后复用现有控制器入口。该 Mixin 必须覆盖 `FishingHook` 子类实例，且继续使用重复收竿保护。
4. 配置目录由 NeoForge `FMLPaths.CONFIGDIR` 改为 Fabric Loader 的配置目录；JSON 格式和版本 3 迁移规则保持一致。
5. NeoForge 配置扩展点在 Fabric 不存在；Fabric 版仍保留 `F9` 原生配置页面，不强制依赖 Mod Menu。若安装 Mod Menu，可在后续版本另加可选入口。
6. `Screen.isPauseScreen()`、网络包回调和 `FishingHook.nibble` Accessor 继续使用 Mixin，因此 ESC 菜单继续钓鱼、多人声音/速度检测及延迟重抛语义保持一致。
7. Fabric 产物使用独立名称 `autofish-fabric-1.2.0.jar`，NeoForge 产物改用明确名称 `autofish-neoforge-1.2.0.jar`，防止玩家把 JAR 放入错误加载器。

### Aquaculture 2 说明

已验证的 Aquaculture 2 2.9.2 是 NeoForge Mod，不能直接装入 Fabric。`neoforge-26.1.2` 分支继续提供完整的 Aquaculture 2 水中/熔岩浮漂兼容；`fabric-26.1.2` 分支保留“`FishingRodItem` 子类或通用钓鱼竿标签”的识别能力，可兼容遵循该约定的 Fabric 鱼竿 Mod，但不宣称兼容 NeoForge Aquaculture 2 JAR。若未来存在独立的 Aquaculture Fabric 端口，需要针对其实体和标签另行验证后再加入兼容声明。

### 实施与验证顺序

1. 初始化本地 Git，提交当前 NeoForge 已验证基线，并创建 `neoforge-26.1.2` 与 `fabric-26.1.2`。
2. 在 Fabric 分支完成加载器适配，确保不改动 NeoForge 分支的源代码和构建配置。
3. 执行 Fabric `clean build` 和现有 6 项单元测试，检查 `fabric.mod.json`、Mixin 配置、许可证与 JAR 内容。
4. 启动 Fabric 26.1.2 客户端，验证 F8、F9、原版单人自动收竿、多人检测及 ESC 菜单继续运行。
5. 分别切回两个分支重新执行构建；NeoForge 分支追加 Aquaculture 2 联合启动回归，最终把两个带加载器后缀的 JAR 放入统一的本地交付目录。
6. 对两个分支进行完整 Code Review 和自我修复，不推送远程仓库、不部署。
